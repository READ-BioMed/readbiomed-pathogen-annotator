# readbiomed-pathogen-annotator

This package has been developed to annotate mentions of pathogens in the scientific literature.

Pathogen identification and characterisation algorithms have been developed using [readbiomed-pathogens-dataset](https://github.com/READ-BioMed/readbiomed-pathogens-dataset).

Identification of pathogens is built on dictionaries using National Center for Biotechnology Information (NCBI) resources and ConceptMapper. This package has code to generate dictionaries and use them for annotation.

Characterisation of pathogens relies on machine learning algorithms. There is python code to train and evaluate [traditional machine learning methods](https://github.com/READ-BioMed/MTIMLExtension) and deep learning models including [longformer](https://github.com/allenai/longformer).

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

## Pathogen characterisation


## Training of categorization models using MTIMLExtension

## Training of categorization models using pytorch models

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
