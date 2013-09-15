// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef PPAPI_HOST_RESOURCE_MESSAGE_FILTER_H_
#define PPAPI_HOST_RESOURCE_MESSAGE_FILTER_H_

#include "base/memory/ref_counted.h"
#include "ppapi/c/pp_stdint.h"
#include "ppapi/host/host_message_context.h"
#include "ppapi/host/ppapi_host_export.h"
#include "ppapi/host/resource_message_handler.h"

namespace base {
class MessageLoopProxy;
class TaskRunner;
}

namespace IPC {
class Message;
}

namespace ppapi {
namespace host {

class ResourceHost;

// This is the base class of resource message filters that can handle resource
// messages on another thread. ResourceHosts can handle most messages
// directly, but if they need to handle something on a different thread it is
// inconvenient. This class makes handling that case easier. This class is
// similar to a BrowserMessageFilter but for resource messages. Note that the
// liftetime of a ResourceHost is managed by a PpapiHost and may be destroyed
// before or while your message is being processed on another thread.
// If this is the case, the message handler will always be called but a reply
// may not be sent back to the host.
//
// To handle a resource message on another thread you should implement a
// subclass as follows:
// class MyMessageFilter : public ResourceMessageFilter {
//  protected:
//   virtual scoped_refptr<base::TaskRunner> OverrideTaskRunnerForMessage(
//       const IPC::Message& message) OVERRIDE {
//     if (message.type() == MyMessage::ID)
//       return BrowserThread::GetMessageLoopProxyForThread(BrowserThread::UI);
//     return NULL;
//   }
//
//   virtual int32_t OnResourceMessageReceived(
//       const IPC::Message& msg,
//       HostMessageContext* context) OVERRIDE {
//     IPC_BEGIN_MESSAGE_MAP(MyMessageFilter, msg)
//       PPAPI_DISPATCH_HOST_RESOURCE_CALL(MyMessage, OnMyMessage)
//     IPC_END_MESSAGE_MAP()
//     return PP_ERROR_FAILED;
//   }
//
//  private:
//   int32_t OnMyMessage(ppapi::host::HostMessageContext* context, ...) {
//     // Will be run on the UI thread.
//   }
// }
//
// The filter should then be added in the resource host using:
// AddFilter(make_scoped_refptr(new MyMessageFilter));
class PPAPI_HOST_EXPORT ResourceMessageFilter
    : public ResourceMessageHandler,
      public base::RefCountedThreadSafe<ResourceMessageFilter> {
 public:
  // This object must be constructed on the same thread that a reply message
  // should be sent, i.e. the IO thread when constructed in the browser process
  // or the main thread when constructed in the renderer process. Since
  // ResourceMessageFilters are usually constructed in the constructor of the
  // owning ResourceHost, this will almost always be the case anyway.
  ResourceMessageFilter();
  // Test constructor. Allows you to specify the message loop which will be used
  // to dispatch replies on.
  ResourceMessageFilter(
      scoped_refptr<base::MessageLoopProxy> reply_thread_message_loop_proxy);

  // Called when a filter is added to a ResourceHost.
  void OnFilterAdded(ResourceHost* resource_host);
  // Called when a filter is removed from a ResourceHost.
  void OnFilterDestroyed();

  // This will dispatch the message handler on the target thread. It returns
  // true if the message was handled by this filter and false otherwise.
  virtual bool HandleMessage(const IPC::Message& msg,
                             HostMessageContext* context) OVERRIDE;

  // This can be called from any thread.
  virtual void SendReply(const ReplyMessageContext& context,
      const IPC::Message& msg) OVERRIDE;

 protected:
  friend class base::RefCountedThreadSafe<ResourceMessageFilter>;
  virtual ~ResourceMessageFilter();

  // If you want the message to be handled on another thread, return a non-null
  // task runner which will target tasks accordingly.
  virtual scoped_refptr<base::TaskRunner> OverrideTaskRunnerForMessage(
      const IPC::Message& message);

 private:
  // This method is posted to the target thread and runs the message handler.
  void DispatchMessage(const IPC::Message& msg,
                       HostMessageContext context);

  // Message loop to send resource message replies on. This will be the message
  // loop proxy of the IO thread for the browser process or the main thread for
  // the renderer process.
  scoped_refptr<base::MessageLoopProxy> reply_thread_message_loop_proxy_;

  // Non-owning pointer to the resource host owning this filter. Should only be
  // accessed from the thread which sends messages to the plugin resource (i.e.
  // the IO thread for the browser process or the main thread for the renderer).
  // This will be NULL upon creation of the filter and is set to the owning
  // ResourceHost when |OnFilterAdded| is called. When the owning ResourceHost
  // is destroyed, |OnFilterDestroyed| is called and this will be set to NULL.
  ResourceHost* resource_host_;

  DISALLOW_COPY_AND_ASSIGN(ResourceMessageFilter);
};

}  // namespace host
}  // namespace ppapi

#endif  // PPAPI_HOST_RESOURCE_MESSAGE_FILTER_H_
