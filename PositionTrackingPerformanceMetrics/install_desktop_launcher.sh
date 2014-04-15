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

export HTPM_DIR=`pwd`;

sudo -n mkdir -p /usr/local/HumanTrackingPerformanceMetrics/dist && \
    sudo -n mkdir -p /usr/local/HumanTrackingPerformanceMetrics/src/htpm_resources && \
    sudo -n cp -a dist/* /usr/local/HumanTrackingPerformanceMetrics/dist && \
    sudo -n cp -a src/htpm_resources/* /usr/local/HumanTrackingPerformanceMetrics/src/htpm_resources && \
    sudo -n chmod a+r -R /usr/local/HumanTrackingPerformanceMetrics/ && \
    sudo -n chmod a+rx  /usr/local/HumanTrackingPerformanceMetrics/ && \
    sudo -n chmod a+rx  /usr/local/HumanTrackingPerformanceMetrics/src && \
    sudo -n chmod a+rx  /usr/local/HumanTrackingPerformanceMetrics/src/htpm_resources && \
    sudo -n chmod a+rx  /usr/local/HumanTrackingPerformanceMetrics/dist && \
    sudo -n chmod a+rx  /usr/local/HumanTrackingPerformanceMetrics/dist/HumanTrackingPerformanceMetrics.jar && \
    export HTPM_DIR=/usr/local/HumanTrackingPerformanceMetrics/;

cat >htpm.desktop <<EOF
#!/usr/bin/env xdg-open
[Desktop Entry]
Version=1.0
Name=Human Tracking Performance Metrics
Icon=${HTPM_DIR}/src/htpm_resources/cropped_screenshot.png
Exec=${JAVA} -jar ${HTPM_DIR}/dist/HumanTrackingPerformanceMetrics.jar
Type=Application
Terminal=true
EOF

chmod a+rx htpm.desktop

if test -d "${HOME}/Desktop/"  ; then
    echo "Copying  htpm.desktop to ${HOME}/Desktop/"
    cp htpm.desktop ${HOME}/Desktop/
    chmod a+rx ${HOME}/Desktop/htpm.desktop
fi

if test -d "${HOME}/.local/share/applications/" ; then
    echo "Copying  htpm.desktop to ${HOME}/.local/share/applications/"
    cp htpm.desktop ${HOME}/.local/share/applications/
    chmod a+rx ${HOME}/.local/share/applications/htpm.desktop
fi

if test -d "/usr/share/applications/" ; then
    echo "Copying  htpm.desktop to /usr/share/applications/"
    sudo -n cp htpm.desktop /usr/share/applications/
    sudo -n chmod a+rx /usr/share/applications/htpm.desktop
fi





