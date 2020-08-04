/*
  Copyright 2020 TypingDNA Inc. (https://www.typingdna.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package com.typingdna.util;

public final class Constants {
    public static final boolean DEBUG = true;

    public static final String PATTERN_OUTPUT_VARIABLE = "TP";
    public static final String DEVICE_TYPE_OUTPUT_VARIABLE = "DEVICE_TYPE";
    public static final String TEXT_ID_OUTPUT_VARIABLE = "TEXT_ID";

    public static final String TYPING_PATTERN = "TDNA_TYPING_PATTERN";
    public static final String DEVICE_TYPE = "TDNA_DEVICE_TYPE";
    public static final String VERIFY_RETRIES = "TDNA_RETRIES";
    public static final String PATTERNS_ENROLLED = "TDNA_PATTERNS_ENROLLED";
    public static final String ENROLLMENTS_LEFT = "TDNA_ENROLLMENTS_LEFT";
    public static final String PREVIOUS_ACTION = "TDNA_PREVIOUS_ACTION";
    public static final String PREVIOUS_TYPING_PATTERNS = "TDNA_PREVIOUS_TYPING_PATTERNS";
    public static final String TEXT_ID = "TDNA_TEXT_ID";
    public static final String TEXT_TO_ENTER = "TDNA_TEXT_TO_ENTER";
    public static final String MESSAGE = "TDNA_MESSAGE";

    public static final String typingPatternVisualizer = "function TypingVisualizer(){this.deltaX=3,this.vpMaxHeight=16,this.typingLength=7,this.vpAlpha=.5,this.timeDown=(new Date).getTime(),this.timeUp=(new Date).getTime(),this.targets={},this.showTDNALogo=!0,this.logoHeight=16,this.scrollOffset=18,this.setStyle=function(t,e){if(void 0!==t)for(var i in e)e.hasOwnProperty(i)&&(t.style[i]=e[i])};var t=this;this.keyDown=function(e){t.timeDown=(new Date).getTime()},this.keyUp=function(e){var i=t.targets[e.target.id];if(void 0!==i){var n=t.timeUp;t.timeUp=(new Date).getTime();var a=Math.min(500,t.timeUp-n)/500,s=Math.min(180,t.timeUp-t.timeDown)/180;if(8==e.keyCode||46==e.keyCode)i.deleteKeyData();else if(13!=e.keyCode&&9!=e.keyCode){var o=2+Math.round(a*(.8*t.vpMaxHeight)),r=t.vpMaxHeight-o,h=Math.round(s*t.vpMaxHeight);h>r&&(h=r);var l=2+i.visualPattern.length*(t.deltaX+1),p=1+t.vpMaxHeight-(o+h),g=t.vpAlpha;t.vpAlpha=a;var c=4*Math.abs(t.vpAlpha-g);c=c>1?.3:1.3-c,i.addKeyData([l,p,t.deltaX,o,c])}}},this.onChange=function(e){var i=t.targets[e.target.id];void 0!==i&&i.updatePosition()}}TypingVisualizer.prototype.removeTarget=function(t){if(void 0!==t){\"object\"!=typeof t&&(t=[t]);for(var e=0;e<t.length;e++){var i;(i=\"string\"==typeof t[e]?document.getElementById(t[e]):t[e])&&this.targets.hasOwnProperty(i.id)&&(this.targets[i.id].removeEventListener(\"input\",this.onChange),delete this.targets[i.id])}}},TypingVisualizer.prototype.addTarget=function(t){if(void 0!==t){\"object\"!=typeof t&&(t=[t]);for(var e=this,i=0;i<t.length;i++){var n;if((n=\"string\"==typeof t[i]?document.getElementById(t[i]):t[i])&&!this.targets.hasOwnProperty(n.id)){var a=this.generateCanvas(n),s=n.parentNode,o=document.createElement(\"DIV\");if(this.setStyle(o,{position:\"relative\"}),a&&s&&o.appendChild(a.container)){n.addEventListener(\"input\",e.onChange),void 0!==n.style.width&&(this.setStyle(o,{position:\"relative\",width:n.style.width}),n.style.width=\"100%\");var r=n.scrollHeight>n.clientHeight?this.scrollOffset:0;this.setStyle(a.container,{position:\"absolute\",width:this.typingLength*(e.deltaX+2)+\"px\",height:\"100%\",right:(this.showTDNALogo?this.logoHeight+8+r:r+6)+\"px\",top:0}),s.insertBefore(o,n),o.appendChild(n);var h,l=a.container.getBoundingClientRect();a.canvas.width=l.width||this.typingLength*(e.deltaX+2),a.canvas.height=Math.min(l.height||this.vpMaxHeight,this.vpMaxHeight),this.setStyle(a.canvas,{\"margin-top\":\"6px\"}),this.setStyle(n,{\"padding-right\":a.canvas.width+6+(this.showTDNALogo?this.logoHeight+4:0)+\"px\"}),this.showTDNALogo&&(h=this.generateTDNALogo(),this.setStyle(h,{position:\"absolute\",right:r+6+\"px\",top:\"0\",\"margin-top\":\"6px\",\"line-height\":this.logoHeight+\"px\"}),o.appendChild(h),\"undefined\"!=typeof tippy&&tippy(h)),this.targets[n.id]={element:n,canvas:a.canvas,canvasContainer:a.container,canvasContext:a.canvas.getContext(\"2d\"),logo:h,visualPattern:[],scrollOffset:18,hasScroll:n.scrollHeight>n.clientHeight,hasVerticalScroll:function(){return this.element.scrollHeight>this.element.clientHeight},clearCanvas:function(){this.canvasContext&&this.canvasContext.clearRect(0,0,120,30)},addKeyData:function(t){this.visualPattern.push(t),this.update()},deleteKeyData:function(){this.visualPattern.pop(),this.update()},updatePosition:function(){var t=this.hasVerticalScroll();this.hasScroll!==t&&(this.hasScroll=t,this.logo.style.right=parseInt(this.logo.style.right)+(t?this.scrollOffset:-this.scrollOffset)+\"px\",this.canvasContainer.style.right=parseInt(this.canvasContainer.style.right)+(t?this.scrollOffset:-this.scrollOffset)+\"px\")},update:function(){this.updatePosition();for(var t=this.visualPattern.slice(-e.typingLength);t.length<e.typingLength;)t.unshift([0,0,0,0,0]);this.clearCanvas();for(var i=0;i<t.length;i++)this.canvasContext.fillStyle=\"rgba(256, 110, 0, \"+t[i][4]+\")\",this.canvasContext.fillRect(i*(e.deltaX+2),t[i][1],t[i][2],t[i][3])}}}}}this.initListeners()}},TypingVisualizer.prototype.generateTDNALogo=function(){var t=document.createElement(\"A\");t.href=\"javascript: void(0)\",t.setAttribute(\"tabindex\",-1),t.setAttribute(\"data-toggle\",\"popover\"),t.setAttribute(\"data-trigger\",\"focus\"),t.setAttribute(\"data-content\",\"Protected by TypingDNA\"),t.setAttribute(\"title\",\"Protected by TypingDNA\"),t.setAttribute(\"data-placement\",\"left\");var e=document.createElement(\"IMG\");return this.setStyle(e,{height:this.logoHeight+\"px\",width:this.logoHeight+\"px\",\"vertical-align\":\"top\"}),e.src=\"https://www.typingdna.com/assets/images/external/icon-48.png\",e.alt=\"Protected by TypingDNA\",t.appendChild(e),t},TypingVisualizer.prototype.generateCanvas=function(t){if(void 0!==t){var e=document.createElement(\"DIV\"),i=document.createElement(\"CANVAS\");return e.appendChild(i),{canvas:i,container:e}}},TypingVisualizer.prototype.init=function(){for(var t in this.targets)this.targets.hasOwnProperty(t)&&(this.targets[t].visualPattern=[]);this.initListeners()},TypingVisualizer.prototype.clearCanvas=function(t){if(void 0===t)this.targets.hasOwnProperty(t)&&this.targets[t].canvas.clearRect(0,0,120,30);else for(var e in this.targets)this.targets.hasOwnProperty(e)&&this.targets[e].canvas&&this.targets[e].canvas.clearRect(0,0,120,30)},TypingVisualizer.prototype.initListeners=function(){document.removeEventListener(\"keyup\",this.keyUp),document.removeEventListener(\"keydown\",this.keyDown),TypingVisualizer.isEmpty(this.targets)||(document.addEventListener(\"keyup\",this.keyUp),document.addEventListener(\"keydown\",this.keyDown))},TypingVisualizer.isEmpty=function(t){for(var e in t)if(t.hasOwnProperty(e))return!1;return!0};";
}
