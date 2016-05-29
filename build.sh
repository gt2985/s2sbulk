#!/bin/bash
mvn package assembly:single
rm deploy/*.*
cp target/s2sb-jar-with-dependencies.jar deploy/s2sb.jar
cp *.properties deploy/
cd deploy
zip s2sb.zip *.*

echo "Done."
