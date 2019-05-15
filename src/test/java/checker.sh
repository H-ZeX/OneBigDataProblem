#!/bin/bash
if [[ $# -lt 2 ]]; then
    echo "need two param, first is outputDirs, second is materialFile" 
    exit 1
fi

cd /home/hzx/IdeaProjects/PingCapHW2/out/test/classes/
/media/hzx/infrastructure/BIN/jdk-11.0.1/bin/java -ea -Xms512m -Xmx4096m operator.Checker $1  $2
