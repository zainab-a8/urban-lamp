/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
 *
 *  This file is free software: you may copy, redistribute and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This file is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jmstudios.redmoon.event

/* EventBus uses classes as events. These are the available events to send */

class filterIsOnChanged()
class dimChanged()
class intensityChanged()
class colorChanged()
class themeChanged()
class customTurnOnTimeChanged()
class customTurnOffTimeChanged()
class lowerBrightnessChanged()
class profileChanged()
class timeToggleChanged()
class useLocationChanged()
class locationChanged()
class sunsetTimeChanged()
class sunriseTimeChanged()
class secureSuspendChanged()

class moveToState(val commandFlag: Int)
class serviceStopped()

class locationServicesDisabled()
class locationAccessDenied()
class changeBrightnessDenied()
