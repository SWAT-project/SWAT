#!/bin/bash

pushd petclinic

gradle clean build -x test

popd
