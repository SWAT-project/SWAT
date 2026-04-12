#FROM ubuntu:24.10
#RUN userdel -r ubuntu
FROM archlinux

SHELL ["/bin/bash", "-c"]

ARG USER_ID=1000
ARG GROUP_ID=1000
ENV TZ=Europe/Berlin

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN groupadd -g $GROUP_ID developer \
    && useradd developer -u $USER_ID -g $GROUP_ID -m -s /bin/bash \
    && echo "developer:pass" | chpasswd \
    && mkdir -p /home/developer/petclinic \
    && chown -R developer:developer /home/developer \
    && chsh -s /bin/bash developer

RUN echo 'Server = https://ftp.halifax.rwth-aachen.de/archlinux/$repo/os/$arch' > /etc/pacman.d/mirrorlist

RUN pacman-key --init && pacman-key --populate # pacman --noconfirm -Sy && pacman --noconfirm -S archlinux-keyring
RUN pacman -Syu --noconfirm && \
    pacman -S --noconfirm \
    git \
    wget \
    unzip \
    jdk17-openjdk \
    gradle


USER developer
WORKDIR /home/developer
COPY --chown=developer:developer build_petclinic_docker.sh .
COPY --chown=developer:developer petclinic /home/developer/petclinic