import datetime
import glob
import numpy as np
import os
import spacy
import time
import torch
import argparse
import torch.nn as nn
import math

from transformers import BigBirdPegasusForSequenceClassification
from transformers import BertForSequenceClassification, AdamW, BertConfig
from transformers import get_linear_schedule_with_warmup
from transformers import BertTokenizer

from sklearn.model_selection import train_test_split

import pandas as pd

from torch.utils.data import TensorDataset, random_split
from sklearn.model_selection import train_test_split

from transformers import AutoModel, AutoTokenizer

from torch.utils.data import DataLoader, RandomSampler, SequentialSampler

from constants import model_name, device, max_len, model_file
from torch.utils import mkldnn as mkldnn_utils

# The outcome is a trained classifier
tokenizer = AutoTokenizer.from_pretrained(model_name)

def encode(sentences):
    input_ids = []
    attention_masks = []

    count = 0
    for sent in sentences:
        encoded_dict = tokenizer.encode_plus(
                sent,  # Sentence to encode.
                add_special_tokens=True,  # Add '[CLS]' and '[SEP]'
                max_length=max_len,  # Pad & truncate all sentences.
                truncation=True,
                padding='max_length',
                pad_to_max_length=True,
                return_attention_mask=True,  # Construct attn. masks.
                return_tensors='pt',  # Return pytorch tensors.
            )

        # Add the encoded sentence to the list.
        input_ids.append(encoded_dict['input_ids'])

        # And its attention mask (simply differentiates padding from non-padding).
        attention_masks.append(encoded_dict['attention_mask'])
        count = count + 1
        if count % 10 == 0:
            print (count, flush=True)

    return torch.cat(input_ids, dim=0), torch.cat(attention_masks, dim=0)

#input_ids, attention_masks = encode(df_training["text"])
#labels = torch.tensor(df_training["target"])
#train_idx, val_idx, train_labels, val_labels = train_test_split([i for i in range(len(labels))], labels, test_size=0.1, train_size=0.9, shuffle=True, stratify=labels)

def read_file(file_name):
    text = []
    label = []

    count = 0

    first = True
    with open(file_name, 'r') as f:
        for s in f.readlines():
            if first:
                first = False
                continue
            t = s.split("|")
            text.append(t[1])
            label.append(1 if t[2][0] == "Y" else 0)

            count = count + 1
            if count % 10 == 0:
                print (count)

    return text, label

print ("Reading files", flush=True)
train_text, train_labels = read_file('train.prep.pipe')
val_text, val_labels = read_file('test.prep.pipe')

print ("Encoding", flush=True)
train_input_ids, train_attention_masks = encode(train_text)
val_input_ids, val_attention_masks = encode(val_text)

print(max([i.size()[0] for i in train_input_ids]))

train_dataset = TensorDataset(train_input_ids, train_attention_masks, torch.tensor(train_labels)) 
val_dataset = TensorDataset(val_input_ids, val_attention_masks, torch.tensor(val_labels))

from torch.utils.data import DataLoader, RandomSampler, SequentialSampler
batch_size = 8 
train_dataloader = DataLoader(
        train_dataset,  # The training samples.
        sampler=RandomSampler(train_dataset),  # Select batches randomly
        batch_size=batch_size  # Trains with this batch size.
    )

validation_dataloader = DataLoader(
        val_dataset,  # The validation samples.
        sampler=SequentialSampler(val_dataset),  # Pull out batches sequentially.
        batch_size=batch_size  # Evaluate with this batch size.
    )

def format_time(elapsed):
    #    Takes a time in seconds and returns a string hh:mm:ss
    # Round to the nearest second.
    elapsed_rounded = int(round(elapsed))

    # Format as hh:mm:ss
    return str(datetime.timedelta(seconds=elapsed_rounded))


def flat_accuracy(preds, labels):
    pred_flat = np.argmax(preds, axis=1).flatten()
    labels_flat = labels.flatten()

    return np.sum(pred_flat == labels_flat) / len(labels_flat)


def f1_calc(preds, labels):
    pred_flat = np.argmax(preds, axis=1).flatten()
    labels_flat = labels.flatten()

    tp = np.sum((pred_flat + labels_flat) == 2)
    tp_fp = np.sum(pred_flat)
    p = np.sum(labels_flat)

    return tp, tp_fp, p

def train():
    ## Create model
    model = BertForSequenceClassification.from_pretrained(
        model_name,
        num_labels=2,
        output_attentions=False,
        output_hidden_states=False#,
        #block_size=16,
        #num_random_blocks=2
    )

    print (model)

    optimizer = AdamW(model.parameters(),
                      lr=2e-5,  # args.learning_rate - default is 5e-5, our notebook had 2e-5
                      eps=1e-8 # args.adam_epsilon  - default is 1e-8.
                      )

    epochs = 10

    scheduler = get_linear_schedule_with_warmup(optimizer,
                                            num_warmup_steps=0,  # Default value in run_glue.py
                                            num_training_steps=len(train_dataloader) * epochs)

    total_t0 = time.time()

    max_f1 = None

    #model = mkldnn_utils.to_mkldnn(model)
    model.to(device)

    # For each epoch...
    for epoch_i in range(0, epochs):
        print("")
        print('======== Epoch {:} / {:} ========'.format(epoch_i + 1, epochs))
        print('Training...', flush=True)

        # Measure how long the training epoch takes.
        t0 = time.time()

        # Reset the total loss for this epoch.
        total_train_loss = 0

        # Set up the model for training
        model.train()

        # For each batch of training data...
        for step, batch in enumerate(train_dataloader):

            # Progress update every 40 batches.
            if step % 40 == 0 and not step == 0:
                # Calculate elapsed time in minutes.
                elapsed = format_time(time.time() - t0)

                # Report progress.
                print('  Batch {:>5,}  of  {:>5,}.    Elapsed: {:}.'.format(step, len(train_dataloader), elapsed), flush=True)

            #b_input_ids = batch[0].to_mkldnn()
            #b_input_mask = batch[1].to_mkldnn()
            #b_labels = batch[2].to_mkldnn()
            b_input_ids = batch[0].to(device)
            b_input_mask = batch[1].to(device)
            b_labels = batch[2].to(device)

            # Clear previous gradients
            model.zero_grad()

            # Forward pass
            output = model(input_ids=b_input_ids,
                            #token_type_ids=None,
                            attention_mask=b_input_mask,
                            labels=b_labels)
            
            loss = output[0]
            total_train_loss += loss.item()

            # Perform a backward pass to calculate the gradients.
            loss.backward()

            # Clip the norm of the gradients to 1.0.
            # This is to help prevent the "exploding gradients" problem.
            torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)

            # Update parameters and take a step using the computed gradient.
            # The optimizer dictates the "update rule"--how the parameters are
            # modified based on their gradients, the learning rate, etc.
            optimizer.step()

            # Update the learning rate.
            scheduler.step()

        # Calculate the average loss over all of the batches.
        avg_train_loss = total_train_loss / len(train_dataloader)

        # Measure how long this epoch took.
        training_time = format_time(time.time() - t0)

        print("")
        print("  Average training loss: {0:.2f}".format(avg_train_loss))
        print("  Training epoch took: {:}".format(training_time))

        # ========================================
        #               Validation
        # ========================================
        print("")
        print("Running Validation...", flush=True)

        t0 = time.time()

        # Put the model in evaluation mode--the dropout layers behave differently
        # during evaluation.
        model.eval()

        # Tracking variables 
        total_eval_accuracy = 0
        total_eval_loss = 0
        nb_eval_steps = 0

        total_tp = 0
        total_tp_fp = 0
        total_p = 0

        # Evaluate data for one epoch
        for batch in validation_dataloader:
            #b_input_ids = batch[0].to_mkldnn()
            #b_input_mask = batch[1].to_mkldnn()
            #b_labels = batch[2].to_mkldnn()
            b_input_ids = batch[0].to(device)
            b_input_mask = batch[1].to(device)
            b_labels = batch[2].to(device)

            with torch.no_grad():
                output = model(b_input_ids,
                                   token_type_ids=None,
                                   attention_mask=b_input_mask,
                                   labels=b_labels)

            loss = output[0]
            logits = output[1]

            # Accumulate the validation loss.
            total_eval_loss += loss.item()

            # Move logits and labels to CPU
            logits = logits.detach().cpu().numpy()
            label_ids = b_labels.to('cpu').numpy()

            total_eval_accuracy += flat_accuracy(logits, label_ids)
            tp, tp_fp, p = f1_calc(logits, label_ids)
            total_tp += tp
            total_tp_fp += tp_fp
            total_p += p

        # Report the final accuracy for this validation run.
        avg_val_accuracy = total_eval_accuracy / len(validation_dataloader)
        print("  Accuracy: {0:.2f}".format(avg_val_accuracy))

        precision = (total_tp / total_tp_fp) * 100
        recall = (total_tp / total_p) * 100
        f1 = 2 * precision * recall / (precision + recall)
        
        if max_f1 == None:
            max_f1 = f1
            print("Saving model, f1 == None")
            torch.save(model.state_dict(), model_file)
        else:
            if max_f1 < f1 or math.isnan(max_f1):
                max_f1 = f1
                print("Saving model, new f1 = {}".format(f1))
                torch.save(model.state_dict(), model_file)

        print("  Precision: {0:.2f}".format(precision))
        print("  Recall: {0:.2f}".format(recall))
        print("  F1: {0:.2f}".format(f1))

        # Calculate the average loss over all of the batches.
        avg_val_loss = total_eval_loss / len(validation_dataloader)

        # Measure how long the validation run took.
        validation_time = format_time(time.time() - t0)

        print("  Validation Loss: {0:.2f}".format(avg_val_loss))
        print("  Validation took: {:}".format(validation_time), flush=True)

    print("")
    print("Training complete!")

    print("Total training took {:} (h:mm:ss)".format(format_time(time.time() - total_t0)))

    return model

model = train()

# Safe model after training
torch.save(model.state_dict(), model_file)
