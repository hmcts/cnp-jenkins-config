FROM jenkins/jenkins:2.164.3-alpine
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

RUN wget https://github.com/hmcts/build-failure-analyzer-plugin/releases/download/1.23.0-hmcts/build-failure-analyzer.hpi -O /usr/share/jenkins/ref/plugins/build-failure-analyzer.jpi \
    && wget https://github.com/hmcts/sonarqube-plugin/releases/download/2.10-hmcts/sonar.hpi -O /usr/share/jenkins/ref/plugins/sonar.jpi

RUN mkdir -p /usr/share/jenkins/ref/.ssh && ssh-keyscan -t rsa github.com >> /usr/share/jenkins/ref/.ssh/known_hosts 
