import {ChangeDetectionStrategy, Component, signal} from "@angular/core";
import {ICellRendererParams} from "ag-grid-community";
import {ICellRendererAngularComp} from "ag-grid-angular";

@Component({
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <button (click)="buttonClicked()">
            Delete
        </button>`,
    styles: [`
        button {
            max-height: 95%;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            border: 1px solid #cbd5e1;
            background: #ffffff;
            color: #0f172a;
            padding: 8px 14px;
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.15s ease;
        }
        button:not(:disabled):hover {
            transform: translateY(-1px);
            box-shadow: 0 10px 25px rgba(99, 102, 241, 0.2);
        }
    `]
})
export class DeleteButtonComponent implements ICellRendererAngularComp {
    constructor() {}

    data: any;
    eventSlug = signal('');

    agInit(params: ICellRendererParams<any, any, any>): void {
        this.data = params.data;
        this.refresh(params);
    }
    refresh(params: ICellRendererParams<any, any, any>): boolean {
        // this.eventSlug.set(params.data?.slug ?? '');
        return true;
    }
    buttonClicked() {
        console.warn('Not implemented!');
    }
}
