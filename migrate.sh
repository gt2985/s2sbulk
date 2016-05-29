#!/bin/bash

# Demonstrates how to migrate data using a defined field map
java -cp s2sb.jar api.salesforce.Runner AccountFieldMap.properties migrate
