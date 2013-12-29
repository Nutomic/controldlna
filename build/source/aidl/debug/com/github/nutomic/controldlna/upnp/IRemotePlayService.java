/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/felix/workspace/controldlna/src/com/github/nutomic/controldlna/upnp/IRemotePlayService.aidl
 */
package com.github.nutomic.controldlna.upnp;
public interface IRemotePlayService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.github.nutomic.controldlna.upnp.IRemotePlayService
{
private static final java.lang.String DESCRIPTOR = "com.github.nutomic.controldlna.upnp.IRemotePlayService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.github.nutomic.controldlna.upnp.IRemotePlayService interface,
 * generating a proxy if needed.
 */
public static com.github.nutomic.controldlna.upnp.IRemotePlayService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.github.nutomic.controldlna.upnp.IRemotePlayService))) {
return ((com.github.nutomic.controldlna.upnp.IRemotePlayService)iin);
}
return new com.github.nutomic.controldlna.upnp.IRemotePlayService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_startSearch:
{
data.enforceInterface(DESCRIPTOR);
android.os.Messenger _arg0;
if ((0!=data.readInt())) {
_arg0 = android.os.Messenger.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.startSearch(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_selectRenderer:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.selectRenderer(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_unselectRenderer:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.unselectRenderer(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setVolume:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.setVolume(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_play:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
this.play(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_pause:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.pause(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_resume:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.resume(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_stop:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.stop(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_seek:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
long _arg2;
_arg2 = data.readLong();
this.seek(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_getItemStatus:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
int _arg2;
_arg2 = data.readInt();
this.getItemStatus(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.github.nutomic.controldlna.upnp.IRemotePlayService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public void startSearch(android.os.Messenger listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((listener!=null)) {
_data.writeInt(1);
listener.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_startSearch, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void selectRenderer(java.lang.String id) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(id);
mRemote.transact(Stub.TRANSACTION_selectRenderer, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void unselectRenderer(java.lang.String id) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(id);
mRemote.transact(Stub.TRANSACTION_unselectRenderer, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setVolume(int volume) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(volume);
mRemote.transact(Stub.TRANSACTION_setVolume, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void play(java.lang.String uri, java.lang.String metadata) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(uri);
_data.writeString(metadata);
mRemote.transact(Stub.TRANSACTION_play, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void pause(java.lang.String sessionId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(sessionId);
mRemote.transact(Stub.TRANSACTION_pause, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void resume(java.lang.String sessionId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(sessionId);
mRemote.transact(Stub.TRANSACTION_resume, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void stop(java.lang.String sessionId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(sessionId);
mRemote.transact(Stub.TRANSACTION_stop, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void seek(java.lang.String sessionId, java.lang.String itemId, long milliseconds) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(sessionId);
_data.writeString(itemId);
_data.writeLong(milliseconds);
mRemote.transact(Stub.TRANSACTION_seek, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void getItemStatus(java.lang.String sessionId, java.lang.String itemId, int requestHash) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(sessionId);
_data.writeString(itemId);
_data.writeInt(requestHash);
mRemote.transact(Stub.TRANSACTION_getItemStatus, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_startSearch = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_selectRenderer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_unselectRenderer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_setVolume = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_play = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_pause = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_resume = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_stop = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_seek = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_getItemStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
}
public void startSearch(android.os.Messenger listener) throws android.os.RemoteException;
public void selectRenderer(java.lang.String id) throws android.os.RemoteException;
public void unselectRenderer(java.lang.String id) throws android.os.RemoteException;
public void setVolume(int volume) throws android.os.RemoteException;
public void play(java.lang.String uri, java.lang.String metadata) throws android.os.RemoteException;
public void pause(java.lang.String sessionId) throws android.os.RemoteException;
public void resume(java.lang.String sessionId) throws android.os.RemoteException;
public void stop(java.lang.String sessionId) throws android.os.RemoteException;
public void seek(java.lang.String sessionId, java.lang.String itemId, long milliseconds) throws android.os.RemoteException;
public void getItemStatus(java.lang.String sessionId, java.lang.String itemId, int requestHash) throws android.os.RemoteException;
}
