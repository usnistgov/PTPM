#!/bin/bash

set -x;

JAVA=`find /usr/local/jdk1.7* -name java | head -n 1`

if test "x${JAVA}" = "x" ; then
    JAVA=`which java`
fi

if test "x${JAVA}" = "x" ; then
    echo "No JAVA program found"
    exit 1;
fi 

export PTPM_DIR=`pwd`;

sudo -n mkdir -p /usr/local/PositionTrackingPerformanceMetrics/dist && \
    sudo -n mkdir -p /usr/local/PositionTrackingPerformanceMetrics/src/ptpm_resources && \
    sudo -n cp -a dist/* /usr/local/PositionTrackingPerformanceMetrics/dist && \
    sudo -n cp -a src/ptpm_resources/* /usr/local/PositionTrackingPerformanceMetrics/src/ptpm_resources && \
    sudo -n chmod a+r -R /usr/local/PositionTrackingPerformanceMetrics/ && \
    sudo -n chmod a+rx  /usr/local/PositionTrackingPerformanceMetrics/ && \
    sudo -n chmod a+rx  /usr/local/PositionTrackingPerformanceMetrics/src && \
    sudo -n chmod a+rx  /usr/local/PositionTrackingPerformanceMetrics/src/ptpm_resources && \
    sudo -n chmod a+rx  /usr/local/PositionTrackingPerformanceMetrics/dist && \
    sudo -n chmod a+rx  /usr/local/PositionTrackingPerformanceMetrics/dist/PositionTrackingPerformanceMetrics.jar && \
    export PTPM_DIR=/usr/local/PositionTrackingPerformanceMetrics/;

cat >ptpm.desktop <<EOF
#!/usr/bin/env xdg-open
[Desktop Entry]
Version=1.0
Name=Position Tracking Performance Metrics
Icon=${PTPM_DIR}/src/ptpm_resources/cropped_screenshot.png
Exec=${JAVA} -jar ${PTPM_DIR}/dist/PositionTrackingPerformanceMetrics.jar
Type=Application
Terminal=true
EOF

chmod a+rx ptpm.desktop

if test -d "${HOME}/Desktop/"  ; then
    echo "Copying  ptpm.desktop to ${HOME}/Desktop/"
    cp ptpm.desktop ${HOME}/Desktop/
    chmod a+rx ${HOME}/Desktop/ptpm.desktop
fi

if test -d "${HOME}/.local/share/applications/" ; then
    echo "Copying  ptpm.desktop to ${HOME}/.local/share/applications/"
    cp ptpm.desktop ${HOME}/.local/share/applications/
    chmod a+rx ${HOME}/.local/share/applications/ptpm.desktop
fi

if test -d "/usr/share/applications/" ; then
    echo "Copying  ptpm.desktop to /usr/share/applications/"
    sudo -n cp ptpm.desktop /usr/share/applications/
    sudo -n chmod a+rx /usr/share/applications/ptpm.desktop
fi





