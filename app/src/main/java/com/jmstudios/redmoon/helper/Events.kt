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

/* EventBus uses classes as events. There are the available events to send */

class pauseStateChanged(val newValue: Boolean) {} 
class dimLevelChanged(val newValue: Int) {}
class intensityLevelChanged(val newValue: Int) {}
class colorChanged(val newValue: Int) {}
class automaticFilterChanged(val newValue: Boolean) {}
class automaticTurnOnChanged(val newValue: String) {}
class automaticTurnOffChanged(val newValue: String) {}
class lowerBrightnessChanged(val newValue: Boolean) {}
class profileChanged(val newValue: Int) {}
class automaticSuspendChanged(val newValue: Boolean) {}

class moveToState(val commandFlag: Int) {}
class serviceStopped() {}
