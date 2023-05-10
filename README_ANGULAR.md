# TypingDNA Authentication Nodes integration with Angular web app <br> <sup>(v1.8.3)</sup>

## How to?

Create an authentication tree that uses the TypingDNA Authentication Nodes as described in the [main documentation](README.md). 

The following integration was created by modifying the [Build a protected Angular web app](https://backstage.forgerock.com/docs/sdks/3.3/blog/build-protected-angular-web-app.html) tutorial
from ForgeRock. Start by creating an Angular application according to it and then follow along.

Your Angular application will need two new components. One for handling the __TextOutputCallback__ and __ScriptTextOutputCallback__, and one for
handling the __HiddenValueCallback__.  

The __TextOutputCallback__ is used for displaying messages to the user (like when a user needs to enroll more typing patterns to be
able to be verified or when authentication fails), and the __ScriptTextOutputCallback__ is used for injecting Javascript code in the
login page. The injected code is the __TypingDNA Javascript Recorder__, and all the logic needed to attach the recorder to the targeted inputs,
displaying the visualizer and disabling copy and paste.  

The __HiddenValueCallback__ is used to create hidden inputs in the login form. They will contain the typing pattern, device type and text id.
Without these, the necessary data for biometric authentication won't get to the backend.

### The component that handles TextOutputCallback and ScriptTextOutputCallback

```typescript
@Component({
  selector: 'app-output-text',
  templateUrl: './output-text.component.html',
})
export class OutputTextComponent implements OnInit {
  /**
   * The callback to be represented as a paragraph of text or script
   */
  @Input() callback?: TextOutputCallback;

  /**
   * The name of the callback
   */
  @Input() name?: string;

  /**
   * The id of the callback
   */
  @Input() id?: number;

  ngOnInit(): void {
    /* if script. The ScriptTextOutputCallback has a message type of 4 */
    if (this.callback?.getMessageType() == '4') {
      const head = document.getElementsByTagName('head');
      const script = document.createElement('script');
      script.type = 'text/javascript';
      script.text = this.callback?.getMessage();
      head[0].appendChild(script);
    }
  }
}
```

Display the message only if the type of the callback is not __script__.

```html
<div class="cstm_form-floating form-floating mb-3" id="callback_{{id}}">
  <span *ngIf="callback?.getMessageType() != '4'">{{callback?.getMessage()}}</span>
</div>
```

### The component that handles HiddenValueCallback

```typescript
@Component({
  selector: 'app-hidden-input',
  templateUrl: './hidden-input.component.html',
})
export class HiddenInputComponent implements OnInit {
  /**
   * The callback to be represented as a hidden input
   */
  @Input() callback?: HiddenValueCallback;

  /**
   * The name of the callback
   */
  @Input() name?: string;

  /**
   * Emits a string representing the hidden input value
   */
  @Output() updatedCallback = new EventEmitter<string>();

  ngOnInit(): void { }

  /**
   * Emit an event to the parent component, passing the hidden value
   * @param event
   */
  updateValue(event: any): void {
    this.updatedCallback.emit(event.target.value);
  }
}
```

The Recorder node will trigger a change event when the value of a hidden input is set. There is no need to iterate through the hidden inputs to set the value of the __HiddenValueCallbacks__.

```html
<div>
  <input
    [id]="callback?.getInputValue()"
    type="hidden"
    [name]="name"
    [defaultValue]="callback?.getInputValue()"
    (change)="updateValue($event)"
  />
</div>
```

### Changes to the login form

Add the previously defined components, __app-output-text__ and __app-hidden-input__, to the login form.

```html
<div id="callbacks">
  <form #callbackForm (ngSubmit)="nextStep(step)" ngNativeValidate class="cstm_form">
    <div *ngFor="let callback of step?.callbacks" v-bind:key="callback.payload._id">
      <container-element [ngSwitch]="callback.getType()">
        <app-text
          *ngSwitchCase="'NameCallback'"
          [callback]="$any(callback)"
          [name]="callback?.payload?.input?.[0]?.name"
          (updatedCallback)="$any(callback).setName($event)"
        >
        </app-text>

        <app-password
          *ngSwitchCase="'PasswordCallback'"
          [callback]="$any(callback)"
          [name]="callback?.payload?.input?.[0]?.name"
          (updatedCallback)="$any(callback).setPassword($event)"
        >
        </app-password>

        <app-output-text
          *ngSwitchCase="'TextOutputCallback'"
          [callback]="$any(callback)"
          [name]="callback?.payload?.input?.[0]?.name"
          [id]="callback?.payload?._id"
        >
        </app-output-text>

        <app-hidden-input
          *ngSwitchCase="'HiddenValueCallback'"
          [callback]="$any(callback)"
          [name]="callback?.payload?.input?.[0]?.name"
          (updatedCallback)="$any(callback).setInputValue($event)"
        >
        </app-hidden-input>

        <app-unknown *ngSwitchDefault [callback]="callback"></app-unknown>
      </container-element>
    </div>
    <app-button id="login" [buttonText]="buttonText" [submittingForm]="submittingForm"> </app-button>
  </form>
</div>
```

Make sure that the __TypingDNA Recorder Node__ has the IDs of the inputs from which it collects the typing patterns, and the
ID of the login button. That's it.
