import { ErrorService } from './error.service';

export class ErrorServiceStub implements ErrorService {
    showError(error: Error): void {
        throw error;
    }
}
