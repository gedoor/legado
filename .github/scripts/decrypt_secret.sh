#!/bin/sh

# 解密文件
gpg --quiet --batch --yes --decrypt --passphrase="$LARGE_SECRET_PASSPHRASE" --output $GITHUB_WORKSPACE/app/gradle.properties $GITHUB_WORKSPACE/.github/secrets/gradle.properties.gpg
echo "Decrypt gradle.properties done"
gpg --quiet --batch --yes --decrypt --passphrase="$LARGE_SECRET_PASSPHRASE" --output $GITHUB_WORKSPACE/app/key.jks $GITHUB_WORKSPACE/.github/secrets/key.jks.gpg
echo "Decrypt key.jks done"
