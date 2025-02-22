# Recipe for SimpleOTA client
# Copyright (c) 2025 Technoculture Research
# SPDX-License-Identifier: MIT

SUMMARY = "SimpleOTA - A/B system update manager with Zenoh communication"
DESCRIPTION = "A lightweight OTA update client for embedded Linux systems with A/B partition support and Zenoh-based communication"
HOMEPAGE = "https://github.com/technoculture/simpleota"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=ba0f4108adfdd5a1621baaee935740de"

SRC_URI = "git://github.com/technoculture/simpleota.git;protocol=https;branch=master"
SRCREV = "${AUTOREV}"
PV = "1.0+git${SRCPV}"

S = "${WORKDIR}/git"

DEPENDS = "openssl"
RDEPENDS:${PN} = " \
    python3-core \
    python3-asyncio \
    python3-json \
    python3-logging \
    python3-fcntl \
    python3-shell \
    python3-multiprocessing \
    python3-zenoh \
    python3-cryptography \
    u-boot-fw-utils \
"

inherit setuptools3 systemd update-rc.d

SYSTEMD_SERVICE:${PN} = "simpleota.service"
INITSCRIPT_NAME = "simpleota"
INITSCRIPT_PARAMS = "defaults 80 20"

do_install:append() {
    # Install systemd service file
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/systemd/simpleota.service ${D}${systemd_system_unitdir}/

    # Install init script for non-systemd systems
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${S}/init/simpleota.init ${D}${sysconfdir}/init.d/simpleota

    # Create configuration directory
    install -d ${D}${sysconfdir}/simpleota
    install -m 0644 ${S}/config/default_config.json ${D}${sysconfdir}/simpleota/config.json.sample

    # Create log directory
    install -d ${D}${localstatedir}/log/simpleota

    # Create state directory
    install -d ${D}${localstatedir}/lib/simpleota

    # Install udev rules for secure update
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${S}/udev/99-simpleota.rules ${D}${sysconfdir}/udev/rules.d/
}

FILES:${PN} += " \
    ${systemd_system_unitdir}/simpleota.service \
    ${sysconfdir}/init.d/simpleota \
    ${sysconfdir}/simpleota \
    ${localstatedir}/log/simpleota \
    ${localstatedir}/lib/simpleota \
    ${sysconfdir}/udev/rules.d/99-simpleota.rules \
"

# Add post-install script to generate keys if they don't exist
pkg_postinst:${PN}() {
    # Generate keys if they don't exist
    if [ ! -f $D${sysconfdir}/simpleota/device.key ]; then
        if [ -n "$D" ]; then
            # For rootfs builds
            echo "Keys will be generated on first boot"
        else
            # For running system
            ${bindir}/simpleota-keygen
        fi
    fi
}
