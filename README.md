# readbiomed-pathogen-annotator

This package has been developed to annotate mentions of pathogens in the scientific literature.

Pathogen identification and characterization algorithms have been developed using [readbiomed-pathogens-dataset](https://github.com/READ-BioMed/readbiomed-pathogens-dataset).

Identification of pathogens is built on dictionaries using National Center for Biotechnology Information (NCBI) resources and ConceptMapper. This package has code to generate dictionaries and use them for annotation.

Characterization of pathogens relies on machine learning algorithms. There is python code to train and evaluate [traditional machine learning methods](https://github.com/READ-BioMed/MTIMLExtension) and deep learning models including [longformer](https://github.com/allenai/longformer).

This package has been tested with Java 11 and Maven 3.6.3.

# Installation

## Installation of the java code

Prior to installing this package, you need to manually install [MTIMLExtention](https://github.com/READ-BioMed/MTIMLExtension) and [readbiomed-ncbi-pathogen-dataset-generation](https://github.com/READ-BioMed/readbiomed-ncbi-pathogen-dataset-generation).

Then, once it is cloned, run `mvn install` from the directory it was cloned into.

## Installation of libraries for the Python code

The libraries required for running the python code can be installed using the `requirements.txt` as explained below.
For optimal performance depending on hardware setup, consider the best installation options for [pytorch](https://pytorch.org).

```
pip install -r requirements.txt
```

# Annotation

## Pathogen characterization

The class [PathogenCharacterizationAnnotator](https://github.com/READ-BioMed/readbiomed-pathogen-annotator/blob/main/src/main/java/readbiomed/annotators/characterization/PathogenCharacterizationAnnotator.java) defines the UIMA annotator for the characterization of pathogens.

An example that uses the PathogenCharacterizationAnnotator is available [here](https://github.com/READ-BioMed/readbiomed-pathogen-annotator/blob/main/src/main/java/readbiomed/annotators/characterization/PathogenExperimenter.java). 
The ground truth is available from the manual annotation from this [repository](https://github.com/READ-BioMed/readbiomed-pathogens-dataset).


```
mvn exec:java -Dexec.mainClass="readbiomed.annotators.characterization.PathogenExperimenter" -Dexec.args="[ConceptMapper_Dictionary] [ground_truth_csv] [articles-txt-format]"
```

A dictionary can be build using the class [DictionaryBuilder](https://github.com/READ-BioMed/readbiomed-pathogen-annotator/blob/main/src/main/java/readbiomed/annotators/dictionary/pathogens/build/DictionaryBuilder.java).
It needs the ncbitaxon.owl file available from the OBO repositories.
The output is an XML dictionary suitable for UIMA's ConceptMapper.

```
mvn exec:java -Dexec.mainClass="readbiomed.annotators.dictionary.pathogens.build.DictionaryBuilder" -Dexec.args="[NCBI_owl_file] [ConceptMapper_dictionary_output_file] [taxonomic_pathogens]"
```

## Categorization models using MTIMLExtension

[MTIMLExtension](https://github.com/READ-BioMed/MTIMLExtension) implements some text classifiers using fast and memory efficient methods for training and annotation in java.
It extends the [MTIML](https://lhncbc.nlm.nih.gov/ii/tools/MTI_ML.html) package.
Training and testing classifiers is explained in great detail [here](https://lhncbc.nlm.nih.gov/ii/tools/MTI_ML.html).
The training process generates a serialised classifier in a compressed file.
The location of the file can be specified in the [pathogen characterization class](https://github.com/READ-BioMed/readbiomed-pathogen-annotator/blob/main/src/main/java/readbiomed/annotators/characterization/PathogenCharacterizationAnnotator.java).
The java code needs to be recompiled running `mvn install` as explained in the installation step.

There are several data sets that have been used for pathogen characterization.

To train a classifier to predict if a citation discusses pathogens is available [here](./data/document-relevant).

To train a classifier to predict pathogen relevance a large data set is available [here](https://zenodo.org/record/5866759).

## Categorization models using pytorch models

To train and use the BERT like models, set the [constants](https://github.com/READ-BioMed/readbiomed-pathogen-annotator/blob/main/src/main/python/constants.py) as needed.
BERT like models are connected to java classes of the pathogen annotator using a server/client architecture.
Once a model is trained using [BERT](https://github.com/READ-BioMed/readbiomed-pathogen-annotator/blob/main/src/main/python/train.py) or [longformer](https://github.com/READ-BioMed/readbiomed-pathogen-annotator/blob/main/src/main/python/train-longformer.py), a server needs to be started.
The same data sets as used in the MTIMLExtension can be used to train the BERT like classifiers.

By default port 5000 is used, the [server](https://github.com/READ-BioMed/readbiomed-pathogen-annotator/blob/main/src/main/python/server.py) code can be updated to change it.
If the port is changed, the java [annotator](https://github.com/READ-BioMed/readbiomed-pathogen-annotator/blob/main/src/main/java/readbiomed/annotators/characterization/PathogenCharacterizationAnnotator.java) needs to be updated and the java code recompiled running `mvn install` as explained in the installation step.

# References

If you use this work in your research, remember to cite it:

```
@article{jimeno2022classifying,
  title={Classifying literature mentions of biological pathogens as experimentally studied using natural language processing},
  author={Jimeno Yepes, Antonio and Verspoor, Karin},
  journal={Journal of Biomedical Semantics},
  year={2022}
}
```
