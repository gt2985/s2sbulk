## Salesforce to Salesforce Bulk Data Migration Tool

Usage: java -cp s2sb.jar api.salesforce.Runner {fieldMapName} {generate || migrate}

# Configure Salesforce API user credentials for both source and destination orgs
config.properties (see config.properties.template in root directory for example)

# To print a suggested field map to console:
java -cp s2sb.jar api.salesforce.Runner FieldMapName.properties generate

Copy/paste field map out to actual field map file.
Add custom mapping commands, such as #resolve

# To migrate data between source and destination orgs using a particular field map:
java -cp s2sb.jar api.salesforce.Runner FieldMapName.properties migrate
