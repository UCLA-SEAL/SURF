FROM meteor/meteor-base:20211013T200759Z_489f5fe


USER root

RUN apt-get update && apt-get install -y git python3 
RUN apt-get install -y software-properties-common
RUN add-apt-repository ppa:openjdk-r/ppa
RUN apt-get update
RUN apt-get install -y openjdk-8-jdk


WORKDIR /usr/src/app/
RUN git clone https://github.com/kanghj/SURF.git

WORKDIR SURF/code/meteor_app
RUN chmod -R 755 .meteor
RUN chown -Rh mt .meteor
RUN chown -Rh mt ..
RUN mkdir .meteor/local
RUN chown -Rh mt .meteor/local
RUN meteor npm install --save pycollections html2canvas bootstrap highlight.js @babel/runtime openai   

USER mt



# Expose any necessary ports (e.g., Meteor runs on port 3000 by default)
EXPOSE 3000

