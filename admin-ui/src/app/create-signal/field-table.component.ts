import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {tuiFormatNumber} from "@taiga-ui/core";
import {AbstractControl, ValidationErrors, ValidatorFn, Validators} from "@angular/forms";

@Component({
  selector: 'app-field-table',
  template: `
    <h3 class="mt-4 text-lg">Fields to extract</h3>
    <p class="mt-4 my-8 text-sm">Define the fields to extract from the feed.  Use the description to instruct the AI engine what type of information to look for


    </p>

    <table
      [tuiTextfieldLabelOutside]="true"
      tuiTable
      class="table"
      [columns]="columns"
      style="border-collapse: collapse"
    >
      <thead>
      <tr>
        <th tuiTh>Field name
        </th>
        <th tuiTh>Description
        </th>
        <th tuiTh>Example (Optional)
        </th>
        <th tuiTh></th>
      </tr>
      </thead>

      <tbody
        tuiTbody
        [data]="extractions"
      >
      <tr
        *ngFor="let item of extractions"
        tuiTr
      >
        <td *tuiCell="'title'" tuiTd>

          <tui-input [ngModel]="item.title"
                     [tuiValidator]="validTypeName"
                     [tuiHint]="[] | tuiFieldErrorContent"
                     (ngModelChange)="onValueChange($event,'title',item)">
            Field Name
          </tui-input>
        </td>
        <td *tuiCell="'description'" tuiTd>
          <tui-input [ngModel]="item.description"
                     [tuiHint]="[] | tuiFieldErrorContent"
                     [tuiValidator]="Validators.required"
                     (ngModelChange)="onValueChange($event,'description',item)">
            Description
          </tui-input>
        </td>
        <td *tuiCell="'sample'" tuiTd>
          <tui-input [ngModel]="item.sample"
                     [tuiHint]="[] | tuiFieldErrorContent"
                     (ngModelChange)="onValueChange($event,'sample',item)"></tui-input>
        </td>
        <td tuiTd *tuiCell="'controls'">
          <button tuiIconButton [icon]="'tuiIconTrash2'" size="s" appearance="icon" (click)="removeRow(item)"></button>
          <!--              <tui-svg src="tuiIconTrash2"></tui-svg>-->

        </td>

      </tr>
      </tbody>
    </table>
    <div class="mt-4">
      <button tuiButton size="s" appearance="outline" (click)="addRow()">Add row</button>
    </div>
  `,
  styleUrls: ['./field-table.component.scss']
})
export class FieldTableComponent {
  readonly options = {updateOn: 'blur'} as const;

  constructor() {
    // this.addRow();
  }


  columns = ['title', 'description', 'sample', 'controls']

  @Input()
  extractions: Extraction[] = [];

  @Output()
  extractionsChange = new EventEmitter<Extraction[]>;

  @Output()
  validChange = new EventEmitter<boolean>;

  @Output()
  errorsChange = new EventEmitter<ValidationErrors | null>();


  private emitUpdate() {
    this.extractionsChange.emit(this.extractions);
  }

  get valid(): boolean {
    return false;
  }


  addRow() {
    this.extractions.push({
      title: '',
      description: '',
      sample: ''
    })
    this.emitUpdate();
  }

  removeRow(element: Extraction) {
    this.extractions.splice(this.extractions.indexOf(element), 1);
    this.emitUpdate();
  }

  onValueChange(value: any, key: keyof Extraction, item: Extraction) {
    item[key] = value;
    this.emitUpdate();
  }

  readonly validTypeName: ValidatorFn = ({value}) => {
    const isValid = typeNameRegex.test(value);
    return isValid ? null : {invalidTypeName: 'Must not contain spaces, and start with a letter'};
  }

  protected readonly Validators = Validators;
}

export interface Extraction {
  title: string;
  description: string;
  sample: string | null;
}

const typeNameRegex = new RegExp('^[a-zA-Z]\\w*$'); // Any character, following by any character or number

