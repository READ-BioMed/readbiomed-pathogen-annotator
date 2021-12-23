# readbiomed-pathogen-annotator

Documentation under development.

This package has been developed to annotate mentions of pathogens in the scientific literature.

Pathogen identification and characterisation algorithms have been developed using [readbiomed-pathogens-dataset](https://github.com/READ-BioMed/readbiomed-pathogens-dataset).

Identification of pathogens is built on dictionaries using National Center for Biotechnology Information (NCBI) resources and ConceptMapper. This package has code to generate dictionaries and use them for annotation.

Characterisation of pathogens relies on machine learning algorithms. There is python code to train and evaluate [traditional machine learning methods](https://github.com/READ-BioMed/MTIMLExtension) and deep learning models including [longformer](https://github.com/allenai/longformer).

This package has been tested with Java 11 and Maven 3.6.3.

To install it, once it is cloned, run mvn install
