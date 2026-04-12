FROM ubuntu:22.04

SHELL ["/bin/bash", "-c"]

ARG USER_ID=1000
ARG GROUP_ID=1000
ENV TZ=Europe/Berlin

ENV PUBLIC_URL=/
ENV REACT_APP_USE_MOCK_API=
ENV REACT_APP_BASE_PATH=/

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN groupadd -g $GROUP_ID developer \
    && useradd developer -u $USER_ID -g $GROUP_ID -m -s /bin/bash \
    && echo "developer:pass" | chpasswd \
    && mkdir -p /home/developer/openhospital/openhospital-ui \
    && chown -R developer:developer /home/developer \
    && chsh -s /bin/bash developer

RUN apt-get update && \
    apt-get install -y \
    git \
    wget \
    unzip \
    curl

RUN cd /opt && wget https://deb.nodesource.com/setup_20.x \
    && chmod +x setup_20.x && ./setup_20.x \
    && apt update \
    && apt install -y nodejs

USER developer
WORKDIR /home/developer/openhospital
COPY build_openhospital_ui.sh /home/developer/openhospital/build_openhospital_ui.sh
COPY clone_openhospital_ui.sh /home/developer/openhospital/clone_openhospital_ui.sh
COPY run_openhospital_ui.sh /home/developer/openhospital/run_openhospital_ui.sh