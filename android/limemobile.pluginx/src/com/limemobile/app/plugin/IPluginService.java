/*
 * Copyright (C) 2014 achellies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.limemobile.app.plugin;

import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.IBinder;

import com.limemobile.app.plugin.internal.PluginClientInfo;

public interface IPluginService {

	public void setDelegate(Service pluginHostService,
			PluginClientInfo pluginPackage);

	public void onCreate();

	public void onStart(Intent intent, int startId);

	public int onStartCommand(Intent intent, int flags, int startId);

	public void onDestroy();

	public void onConfigurationChanged(Configuration newConfig);

	public void onLowMemory();

	public void onTrimMemory(int level);

	public IBinder onBind(Intent intent);

	public boolean onUnbind(Intent intent);

	public void onRebind(Intent intent);

	public void unbindService(ServiceConnection conn);

	public void onTaskRemoved(Intent rootIntent);

	public String getPackageName();
}
