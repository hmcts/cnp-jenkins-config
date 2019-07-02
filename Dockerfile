FROM jenkins/jenkins:2.176.1-alpine

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

RUN mkdir -p /usr/share/jenkins/ref/.ssh && ssh-keyscan -t rsa github.com >> /usr/share/jenkins/ref/.ssh/known_hosts 
