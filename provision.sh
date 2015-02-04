#!/bin/bash
#
# Copyright (C) 2014 Christian Trimble (xiantrimble@gmail.com)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


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
