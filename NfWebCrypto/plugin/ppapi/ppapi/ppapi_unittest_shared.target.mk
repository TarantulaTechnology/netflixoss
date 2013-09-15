# This file is generated by gyp; do not edit.

TOOLSET := target
TARGET := ppapi_unittest_shared
DEFS_Debug := \
	'-D_FILE_OFFSET_BITS=64' \
	'-DCHROMIUM_BUILD' \
	'-DUSE_DEFAULT_RENDER_THEME=1' \
	'-DUSE_LIBJPEG_TURBO=1' \
	'-DUSE_NSS=1' \
	'-DENABLE_ONE_CLICK_SIGNIN' \
	'-DGTK_DISABLE_SINGLE_INCLUDES=1' \
	'-DENABLE_REMOTING=1' \
	'-DENABLE_WEBRTC=1' \
	'-DENABLE_PEPPER_THREADING' \
	'-DENABLE_CONFIGURATION_POLICY' \
	'-DENABLE_INPUT_SPEECH' \
	'-DENABLE_NOTIFICATIONS' \
	'-DENABLE_GPU=1' \
	'-DENABLE_EGLIMAGE=1' \
	'-DUSE_SKIA=1' \
	'-DENABLE_TASK_MANAGER=1' \
	'-DENABLE_WEB_INTENTS=1' \
	'-DENABLE_EXTENSIONS=1' \
	'-DENABLE_PLUGIN_INSTALLATION=1' \
	'-DENABLE_PLUGINS=1' \
	'-DENABLE_SESSION_SERVICE=1' \
	'-DENABLE_THEMES=1' \
	'-DENABLE_BACKGROUND=1' \
	'-DENABLE_AUTOMATION=1' \
	'-DENABLE_GOOGLE_NOW=1' \
	'-DENABLE_LANGUAGE_DETECTION=1' \
	'-DENABLE_PRINTING=1' \
	'-DENABLE_CAPTIVE_PORTAL_DETECTION=1' \
	'-DGL_GLEXT_PROTOTYPES' \
	'-DSK_BUILD_NO_IMAGE_ENCODE' \
	'-DSK_DEFERRED_CANVAS_USES_GPIPE=1' \
	'-DGR_GL_CUSTOM_SETUP_HEADER="GrGLConfig_chrome.h"' \
	'-DGR_AGGRESSIVE_SHADER_OPTS=1' \
	'-DSK_USE_POSIX_THREADS' \
	'-DU_USING_ICU_NAMESPACE=0' \
	'-DU_STATIC_IMPLEMENTATION' \
	'-DUNIT_TEST' \
	'-DGTEST_HAS_RTTI=0' \
	'-D__STDC_CONSTANT_MACROS' \
	'-D__STDC_FORMAT_MACROS' \
	'-DDYNAMIC_ANNOTATIONS_ENABLED=1' \
	'-DWTF_USE_DYNAMIC_ANNOTATIONS=1' \
	'-D_DEBUG'

# Flags passed to all source files.
CFLAGS_Debug := \
	-fstack-protector \
	--param=ssp-buffer-size=4 \
	-Werror \
	-pthread \
	-fno-exceptions \
	-fno-strict-aliasing \
	-Wall \
	-Wno-unused-parameter \
	-Wno-missing-field-initializers \
	-fvisibility=hidden \
	-pipe \
	-fPIC \
	-fPIC \
	-fvisibility=hidden \
	-pthread \
	-I/usr/include/glib-2.0 \
	-I/usr/lib/glib-2.0/include \
	-pthread \
	-D_REENTRANT \
	-I/usr/include/glib-2.0 \
	-I/usr/lib/glib-2.0/include \
	-I/usr/include/gtk-2.0 \
	-I/usr/lib/gtk-2.0/include \
	-I/usr/include/atk-1.0 \
	-I/usr/include/cairo \
	-I/usr/include/pango-1.0 \
	-I/usr/include/gio-unix-2.0/ \
	-I/usr/include/pixman-1 \
	-I/usr/include/freetype2 \
	-I/usr/include/directfb \
	-I/usr/include/libpng12 \
	-O0 \
	-g

# Flags passed to only C files.
CFLAGS_C_Debug :=

# Flags passed to only C++ files.
CFLAGS_CC_Debug := \
	-fno-rtti \
	-fno-threadsafe-statics \
	-fvisibility-inlines-hidden \
	-Wsign-compare

INCS_Debug := \
	-Ithird_party/icu/public/common \
	-I. \
	-Ithird_party/khronos \
	-Igpu \
	-Ithird_party/WebKit/Source/Platform/chromium \
	-Ithird_party/WebKit/Source/Platform/chromium \
	-I$(obj)/gen/webcore_headers \
	-Iskia/config \
	-Ithird_party/skia/src/core \
	-Ithird_party/skia/include/config \
	-Ithird_party/skia/include/core \
	-Ithird_party/skia/include/effects \
	-Ithird_party/skia/include/pdf \
	-Ithird_party/skia/include/gpu \
	-Ithird_party/skia/include/gpu/gl \
	-Ithird_party/skia/include/pipe \
	-Ithird_party/skia/include/ports \
	-Ithird_party/skia/include/utils \
	-Iskia/ext \
	-Ithird_party/npapi \
	-Ithird_party/npapi/bindings \
	-Iv8/include \
	-Itesting/gmock/include \
	-Itesting/gtest/include

DEFS_Release := \
	'-D_FILE_OFFSET_BITS=64' \
	'-DCHROMIUM_BUILD' \
	'-DUSE_DEFAULT_RENDER_THEME=1' \
	'-DUSE_LIBJPEG_TURBO=1' \
	'-DUSE_NSS=1' \
	'-DENABLE_ONE_CLICK_SIGNIN' \
	'-DGTK_DISABLE_SINGLE_INCLUDES=1' \
	'-DENABLE_REMOTING=1' \
	'-DENABLE_WEBRTC=1' \
	'-DENABLE_PEPPER_THREADING' \
	'-DENABLE_CONFIGURATION_POLICY' \
	'-DENABLE_INPUT_SPEECH' \
	'-DENABLE_NOTIFICATIONS' \
	'-DENABLE_GPU=1' \
	'-DENABLE_EGLIMAGE=1' \
	'-DUSE_SKIA=1' \
	'-DENABLE_TASK_MANAGER=1' \
	'-DENABLE_WEB_INTENTS=1' \
	'-DENABLE_EXTENSIONS=1' \
	'-DENABLE_PLUGIN_INSTALLATION=1' \
	'-DENABLE_PLUGINS=1' \
	'-DENABLE_SESSION_SERVICE=1' \
	'-DENABLE_THEMES=1' \
	'-DENABLE_BACKGROUND=1' \
	'-DENABLE_AUTOMATION=1' \
	'-DENABLE_GOOGLE_NOW=1' \
	'-DENABLE_LANGUAGE_DETECTION=1' \
	'-DENABLE_PRINTING=1' \
	'-DENABLE_CAPTIVE_PORTAL_DETECTION=1' \
	'-DGL_GLEXT_PROTOTYPES' \
	'-DSK_BUILD_NO_IMAGE_ENCODE' \
	'-DSK_DEFERRED_CANVAS_USES_GPIPE=1' \
	'-DGR_GL_CUSTOM_SETUP_HEADER="GrGLConfig_chrome.h"' \
	'-DGR_AGGRESSIVE_SHADER_OPTS=1' \
	'-DSK_USE_POSIX_THREADS' \
	'-DU_USING_ICU_NAMESPACE=0' \
	'-DU_STATIC_IMPLEMENTATION' \
	'-DUNIT_TEST' \
	'-DGTEST_HAS_RTTI=0' \
	'-D__STDC_CONSTANT_MACROS' \
	'-D__STDC_FORMAT_MACROS' \
	'-DNDEBUG' \
	'-DNVALGRIND' \
	'-DDYNAMIC_ANNOTATIONS_ENABLED=0' \
	'-D_FORTIFY_SOURCE=2'

# Flags passed to all source files.
CFLAGS_Release := \
	-fstack-protector \
	--param=ssp-buffer-size=4 \
	-Werror \
	-pthread \
	-fno-exceptions \
	-fno-strict-aliasing \
	-Wall \
	-Wno-unused-parameter \
	-Wno-missing-field-initializers \
	-fvisibility=hidden \
	-pipe \
	-fPIC \
	-fPIC \
	-fvisibility=hidden \
	-pthread \
	-I/usr/include/glib-2.0 \
	-I/usr/lib/glib-2.0/include \
	-pthread \
	-D_REENTRANT \
	-I/usr/include/glib-2.0 \
	-I/usr/lib/glib-2.0/include \
	-I/usr/include/gtk-2.0 \
	-I/usr/lib/gtk-2.0/include \
	-I/usr/include/atk-1.0 \
	-I/usr/include/cairo \
	-I/usr/include/pango-1.0 \
	-I/usr/include/gio-unix-2.0/ \
	-I/usr/include/pixman-1 \
	-I/usr/include/freetype2 \
	-I/usr/include/directfb \
	-I/usr/include/libpng12 \
	-O2 \
	-fno-ident \
	-fdata-sections \
	-ffunction-sections

# Flags passed to only C files.
CFLAGS_C_Release :=

# Flags passed to only C++ files.
CFLAGS_CC_Release := \
	-fno-rtti \
	-fno-threadsafe-statics \
	-fvisibility-inlines-hidden \
	-Wsign-compare

INCS_Release := \
	-Ithird_party/icu/public/common \
	-I. \
	-Ithird_party/khronos \
	-Igpu \
	-Ithird_party/WebKit/Source/Platform/chromium \
	-Ithird_party/WebKit/Source/Platform/chromium \
	-I$(obj)/gen/webcore_headers \
	-Iskia/config \
	-Ithird_party/skia/src/core \
	-Ithird_party/skia/include/config \
	-Ithird_party/skia/include/core \
	-Ithird_party/skia/include/effects \
	-Ithird_party/skia/include/pdf \
	-Ithird_party/skia/include/gpu \
	-Ithird_party/skia/include/gpu/gl \
	-Ithird_party/skia/include/pipe \
	-Ithird_party/skia/include/ports \
	-Ithird_party/skia/include/utils \
	-Iskia/ext \
	-Ithird_party/npapi \
	-Ithird_party/npapi/bindings \
	-Iv8/include \
	-Itesting/gmock/include \
	-Itesting/gtest/include

OBJS := \
	$(obj).target/$(TARGET)/ppapi/proxy/ppapi_proxy_test.o \
	$(obj).target/$(TARGET)/ppapi/proxy/resource_message_test_sink.o \
	$(obj).target/$(TARGET)/ppapi/shared_impl/test_globals.o

# Add to the list of files we specially track dependencies for.
all_deps += $(OBJS)

# CFLAGS et al overrides must be target-local.
# See "Target-specific Variable Values" in the GNU Make manual.
$(OBJS): TOOLSET := $(TOOLSET)
$(OBJS): GYP_CFLAGS := $(DEFS_$(BUILDTYPE)) $(INCS_$(BUILDTYPE))  $(CFLAGS_$(BUILDTYPE)) $(CFLAGS_C_$(BUILDTYPE))
$(OBJS): GYP_CXXFLAGS := $(DEFS_$(BUILDTYPE)) $(INCS_$(BUILDTYPE))  $(CFLAGS_$(BUILDTYPE)) $(CFLAGS_CC_$(BUILDTYPE))

# Suffix rules, putting all outputs into $(obj).

$(obj).$(TOOLSET)/$(TARGET)/%.o: $(srcdir)/%.cc FORCE_DO_CMD
	@$(call do_cmd,cxx,1)

# Try building from generated source, too.

$(obj).$(TOOLSET)/$(TARGET)/%.o: $(obj).$(TOOLSET)/%.cc FORCE_DO_CMD
	@$(call do_cmd,cxx,1)

$(obj).$(TOOLSET)/$(TARGET)/%.o: $(obj)/%.cc FORCE_DO_CMD
	@$(call do_cmd,cxx,1)

# End of this set of suffix rules
### Rules for final target.
LDFLAGS_Debug := \
	-Wl,-z,now \
	-Wl,-z,relro \
	-pthread \
	-Wl,-z,noexecstack \
	-fPIC \
	-Wl,--threads \
	-Wl,--thread-count=4 \
	-B$(builddir)/../../third_party/gold \
	-L$(builddir) \
	-Wl,--icf=none

LDFLAGS_Release := \
	-Wl,-z,now \
	-Wl,-z,relro \
	-pthread \
	-Wl,-z,noexecstack \
	-fPIC \
	-Wl,--threads \
	-Wl,--thread-count=4 \
	-B$(builddir)/../../third_party/gold \
	-L$(builddir) \
	-Wl,--icf=none \
	-Wl,-O1 \
	-Wl,--as-needed \
	-Wl,--gc-sections

LIBS := \
	

$(obj).target/ppapi/libppapi_unittest_shared.a: GYP_LDFLAGS := $(LDFLAGS_$(BUILDTYPE))
$(obj).target/ppapi/libppapi_unittest_shared.a: LIBS := $(LIBS)
$(obj).target/ppapi/libppapi_unittest_shared.a: TOOLSET := $(TOOLSET)
$(obj).target/ppapi/libppapi_unittest_shared.a: $(OBJS) FORCE_DO_CMD
	$(call do_cmd,alink_thin)

all_deps += $(obj).target/ppapi/libppapi_unittest_shared.a
# Add target alias
.PHONY: ppapi_unittest_shared
ppapi_unittest_shared: $(obj).target/ppapi/libppapi_unittest_shared.a

# Add target alias to "all" target.
.PHONY: all
all: ppapi_unittest_shared

