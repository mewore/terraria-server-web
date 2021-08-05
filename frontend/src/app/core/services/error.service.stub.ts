import { ErrorService } from './error.service';

export class ErrorServiceStub implements ErrorService {
    showError(error: Error | string): void {
        throw typeof error === 'string' ? new Error(error) : error;
    }
}
