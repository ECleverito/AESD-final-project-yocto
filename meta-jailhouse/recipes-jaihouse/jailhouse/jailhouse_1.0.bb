SUMMARY = "Jailhouse, NVIDIA TX2 Port"
HOMEPAGE = "https://github.com/siemens/jailhouse"
SECTION = "jailhouse"
LICENSE = "GPL-2.0"

LIC_FILES_CHKSUM = "file://COPYING;md5=9fa7f895f96bde2d47fd5b7d95b6ba4d \
"

SRCBRANCH = "master"
SRCREV = "54bac8558279e1aadb5bb5fe66941511aa34913a"

SRC_URI = "git://github.com/evidence/linux-jailhouse-jetson.git;protocol=ssh;branch=${SRCBRANCH}"
SRC_URI[sha256sum] = "fd305d23779f4ae54aa184052d3995874bfb077e397cc306554ece4601206b7c"

DEPENDS = " \
    make-native \
    python3-mako-native \
    python3-mako \
    dtc-native \
"

inherit module python3native bash-completion deploy setuptools3

S = "${WORKDIR}/git"
B = "${S}"

JH_ARCH = "arm64"
JH_DATADIR ?= "${datadir}/jailhouse"
CELL_DIR ?= "${JH_DATADIR}/cells"
CELLCONF_DIR ?= "${JH_DATADIR}/configs"
INMATES_DIR ?= "${JH_DATADIR}/inmates"

JH_CONFIG ?= "${S}/ci/jailhouse-config-x86.h"
JH_CONFIG_x86 ?= "${S}/ci/jailhouse-config-x86.h"
JH_CONFIG_x86-64 ?= "${S}/ci/jailhouse-config-x86.h"
JH_CONFIG_arm ?= "${S}/ci/jailhouse-config-banana-pi.h"

do_configure() {
   if [ -d ${STAGING_DIR_HOST}/${CELLCONF_DIR} ];
   then
      cp "${STAGING_DIR_HOST}/${CELLCONF_DIR}/"*.c ${S}/configs/${ARCH}/
   fi
}

USER_SPACE_CFLAGS = '${CFLAGS} -DLIBEXECDIR=\\\"${libexecdir}\\\" \
		  -DJAILHOUSE_VERSION=\\\"$JAILHOUSE_VERSION\\\" \
		  -Wall -Wextra -Wmissing-declarations -Wmissing-prototypes -Werror \
		  -I../driver'

TOOLS_SRC_DIR = "${S}/tools"
TOOLS_OBJ_DIR = "${S}/tools"

FILESEXTRAPATHS_prepend := "${THISDIR}/files:"
FILES_${PN} += "${sysconfdir}/include/jailhouse/config.h"

do_compile() {
    unset LDFLAGS
    oe_runmake V=1 CC="${CC}" \
        ARCH=${JH_ARCH} CROSS_COMPILE=${TARGET_PREFIX} \
        KDIR=${STAGING_KERNEL_BUILDDIR}

    cd ${TOOLS_SRC_DIR}
    export JAILHOUSE_VERSION=$(cat ../VERSION)
    oe_runmake V=1 \
        CFLAGS="${USER_SPACE_CFLAGS}" \
        src=${TOOLS_SRC_DIR} obj=${TOOLS_OBJ_DIR} \
        ${TOOLS_OBJ_DIR}/jailhouse-config-collect ${TOOLS_OBJ_DIR}/jailhouse
}

do_install() {
    oe_runmake \
        PYTHON=python3 \
        V=1 \
        LDFLAGS="" \
        CC="${CC}" \
        ARCH=${JH_ARCH} \
        CROSS_COMPILE=${TARGET_PREFIX} \
        KDIR=${STAGING_KERNEL_BUILDDIR} \
        DESTDIR=${D} install

    install -d ${D}${CELL_DIR}
    install ${B}/configs/${JH_ARCH}/*.cell ${D}${CELL_DIR}/

    install -d ${D}${INMATES_DIR}/tools/${JH_ARCH}
    install ${B}/inmates/demos/${JH_ARCH}/*.bin ${D}${INMATES_DIR}

    install -d ${D}${JH_DATADIR}/tools
    install ${B}/tools/jailhouse-cell-linux ${D}${JH_DATADIR}/tools
    install ${B}/tools/jailhouse-cell-stats ${D}${JH_DATADIR}/tools
    install ${B}/tools/jailhouse-config-collect ${D}${JH_DATADIR}/tools
    install ${B}/tools/jailhouse-config-create ${D}${JH_DATADIR}/tools
    install ${B}/tools/jailhouse-gcov-extract ${D}${JH_DATADIR}/tools
    install ${B}/tools/jailhouse-hardware-check ${D}${JH_DATADIR}/tools
    install ${B}/inmates/tools/${JH_ARCH}/linux-loader.bin ${D}${INMATES_DIR}/tools/${JH_ARCH}

    install -d ${D}/include/jailhouse/ 
    install -m 0755 ${WORKDIR}/config.h ${D}/include/jailhouse/
}

PACKAGE_BEFORE_PN = "kernel-module-jailhouse pyjailhouse"

FILES_${PN} += "${base_libdir}/firmware ${libexecdir} ${sbindir} ${JH_DATADIR}"
FILES_pyjailhouse = "${PYTHON_SITEPACKAGES_DIR}"

RDEPENDS_${PN} += " \
    python3-curses \
    python3-datetime \
    python3-mmap \
"

RDEPENDS_pyjailhouse = " \
    python3-core \
    python3-ctypes \
    python3-fcntl \
    python3-shell \
"

INSANE_SKIP_${PN} = "ldflags"

COMPATIBLE_MACHINE = "jetson-tx2-devkit"
