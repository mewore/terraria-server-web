import { AuthenticatedUser } from '../types';
import { StorageService } from './storage.service';

export class StorageServiceStub implements StorageService {
    user?: AuthenticatedUser;
}
