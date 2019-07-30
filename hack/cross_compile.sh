#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

location=$(dirname $0)
rootdir=$(realpath ${location}/..)

basename=yaks

if [ "$#" -ne 1 ]; then
    echo "usage: $0 version"
    exit 1
fi

version=$1

cross_compile () {
	local label=$1
	local extension=""
	export GOOS=$2
	export GOARCH=$3

	if [ "${GOOS}" == "windows" ]; then
		extension=".exe"
	fi

	echo "Generating ${label}${extension}..."

	go build -o ${rootdir}/${label}${extension} ./cmd/manager/...
}

cross_compile ${basename}-${version}-linux-64bit linux amd64
cross_compile ${basename}-${version}-mac-64bit darwin amd64
cross_compile ${basename}-${version}-windows-64bit windows amd64
