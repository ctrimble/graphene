#!/bin/bash

# Java 8 http://www.webupd8.org/2012/09/install-oracle-java-8-in-ubuntu-via-ppa.html

# add source for Java 8
sudo add-apt-repository -y ppa:webupd8team/java

sudo apt-get update

# install Java 8
echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
sudo apt-get install oracle-java8-installer --assume-yes

# install OpenJDK Java 7 for elasticsearch
sudo apt-get install openjdk-7-jdk --assume-yes

java --version
