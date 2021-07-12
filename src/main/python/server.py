from constants import model_name, device, max_len, model_file
from transformers import BertForSequenceClassification
from transformers import AutoTokenizer

import numpy as np
import torch
from torch.nn.functional import softmax

tokenizer = AutoTokenizer.from_pretrained(model_name)

model = BertForSequenceClassification.from_pretrained(
        model_name,
        num_labels=2,
        output_attentions=False,
        output_hidden_states=False,
    )

model.load_state_dict(torch.load(model_file))
model.to(device)
model.eval()

def predict (string):
    # We assume that there is already a trained model
    model.eval()

    encoded_dict = tokenizer.encode_plus(
                                string,  # Sentence to encode.
                                add_special_tokens=True,  # Add '[CLS]' and '[SEP]'
                                max_length=max_len,  # Pad & truncate all sentences.
                                truncation=True,
                                padding='max_length',
                                pad_to_max_length=False,
                                return_attention_mask=True,  # Construct attn. masks.
                                return_tensors='pt',  # Return pytorch tensors.
                            )
                            
    input = torch.cat([encoded_dict['input_ids']], dim=0).to(device)
    attention_mask=torch.cat([encoded_dict['attention_mask']], dim=0).to(device)

    with torch.no_grad():
        outputs = model(input,
                        token_type_ids=None,
                        attention_mask=attention_mask)

        (logits, ) = outputs
     
        return str(softmax(logits).detach().cpu().numpy()[0][1])
                 
        #logits = logits.detach().cpu().numpy()
        #return str(np.argmax(logits, axis=1).flatten())

from flask import Flask, request

app = Flask(__name__)

@app.route("/")
def home():
    return "<h1>Running BERT on Flask!</h1>"

@app.route("/predict", methods = ['POST', 'GET'])
def predictCall():
    if request.method == 'POST':
        string = request.form['string']
    else:
        string = request.args.get('string')

    return predict(string)
  
app.run()
