import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component } from "@angular/core";
import { ICellRendererParams } from "ag-grid-community";
import { ICellRendererAngularComp } from "ag-grid-angular";

@Component({
    standalone: true,
    imports: [CommonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
            <button>
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
    constructor(
    ) {}

    agInit(params: ICellRendererParams<any, any, any>): void {
    }
    refresh(params: ICellRendererParams<any, any, any>): boolean {
        return true;
    }
}
