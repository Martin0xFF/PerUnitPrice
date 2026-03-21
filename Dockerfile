FROM debian:bookworm-slim

# Avoid tzdata interactive prompt
ENV DEBIAN_FRONTEND=noninteractive

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    git \
    build-essential \
    openjdk-17-jdk \
    wget \
    python3 \
    pkg-config \
    libssl-dev \
    && rm -rf /var/lib/apt/lists/*

# Setup Android SDK
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip -O cmdline-tools.zip && \
    unzip -q cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm cmdline-tools.zip

# Accept licenses and install Android components
# We use ndk 26.1.10909125 which is modern and stable
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "ndk;26.1.10909125"

# Setup Rust
ENV RUSTUP_HOME=/opt/rustup \
    CARGO_HOME=/opt/cargo \
    PATH=/opt/cargo/bin:$PATH
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y

# Add Android targets for cross-compilation and install cargo-ndk
RUN rustup target add \
    aarch64-linux-android \
    armv7-linux-androideabi \
    i686-linux-android \
    x86_64-linux-android && \
    cargo install cargo-ndk

# Install Gradle
RUN wget -q https://services.gradle.org/distributions/gradle-8.1.1-bin.zip -O gradle.zip && \
    unzip -q gradle.zip -d /opt/gradle && \
    rm gradle.zip
ENV PATH=${PATH}:/opt/gradle/gradle-8.1.1/bin

# Set working directory
WORKDIR /app

# Entry point
CMD ["/bin/bash"]
