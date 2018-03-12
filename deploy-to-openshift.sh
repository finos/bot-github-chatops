#!/bin/bash

BRANCH_NAME=$1
TRAVIS_PULL_REQUEST=$2

# NodePorts must be within 30000-32767 range - read more on https://docs.openshift.com/container-platform/latest/dev_guide/expose_service/expose_internal_ip_nodeport.html

if [[ $BRANCH_NAME =~ master ]]; then
	export SYMPHONY_POD_HOST="foundation.symphony.com"
	export SYMPHONY_API_HOST="foundation-api.symphony.com"
    export BOT_NAME="bot-github-chatops-prod"
    export OC_PROJECT_NAME="ssf-prod"
    export JOLOKIA_NODE_PORT=30020

elif [[ $BRANCH_NAME =~ dev ]]; then
	# Reset Openshift env on every build, for testing purposes
	# export OC_DELETE_LABEL="app=bot-github-chatops-dev"
	export SYMPHONY_POD_HOST="foundation-dev.symphony.com"
	export SYMPHONY_API_HOST="foundation-dev-api.symphony.com"
    export BOT_NAME="bot-github-chatops-dev"
    export OC_PROJECT_NAME="ssf-dev"
    export JOLOKIA_NODE_PORT=30021
else
	echo "Skipping deployment for branch $BRANCH_NAME"
	exit 0
fi

export OC_BINARY_FOLDER="./target/oc"
export OC_ENDPOINT="https://api.pro-us-east-1.openshift.com"
export OC_TEMPLATE_PROCESS_ARGS="BOT_NAME,SYMPHONY_POD_HOST,SYMPHONY_API_HOST,JOLOKIA_NODE_PORT"

if [[ "$TRAVIS_PULL_REQUEST" = "false" ]]; then
	curl -s https://raw.githubusercontent.com/symphonyoss/contrib-toolbox/master/scripts/oc-deploy.sh | bash
fi
