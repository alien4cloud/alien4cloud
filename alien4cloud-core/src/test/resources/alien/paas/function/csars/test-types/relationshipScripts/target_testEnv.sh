#!/bin/bash


echo "MY_HOSTNAME : $MY_HOSTNAME"

echo "SOURCE_HOSTNAME : $SOURCE_HOSTNAME"

echo "MY_IP : $MY_IP"

echo "SOURCE_IP : $SOURCE_IP"

echo "SOURCE : $SOURCE"

echo "SOURCE_NAME : $SOURCE_NAME"

echo "SOURCE_SERVICE_NAME : $SOURCE_SERVICE_NAME"

echo "SOURCES : $SOURCES"

echo "TARGET : $TARGET"

echo "TARGET_NAME : $TARGET_NAME"

echo "TARGET_SERVICE_NAME : $TARGET_SERVICE_NAME"

echo "TARGETS : $TARGETS"

count=0
IFS=',' read -ra _SOURCES <<< "$SOURCES"
for i in "${_SOURCES[@]}"; do
  ((count++))
done

echo "Nb of sources is $count"

count=0
IFS=',' read -ra _TARGETS <<< "$TARGETS"
for i in "${_TARGETS[@]}"; do
  ((count++))
done

echo "Nb of targets is $count"

echo "$TARGET ( $MY_IP )... $SOURCE ( $SOURCE_IP )"

# def sourcesArray = SOURCES.split(",")
# def nbSource = sourcesArray.length;
# echo "Nb of sources is $nbSource: $sourcesArray"

# def targetsArray = TARGETS.split(",")
# def nbTarget = targetsArray.length;
# echo "Nb of targets is $nbTarget: $targetsArray"

# targetsArray.each{
  # def name = it+"_MY_IP"
  
  # echo "$name : $binding.getVariable(name)"


# return TARGET+"...."+SOURCE;
