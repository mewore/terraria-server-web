import { AccountTypeEntity } from 'src/generated/backend';

export interface AuthenticatedUser {
    username: string;
    sessionToken: string;
    authData: string;
    accountType?: Omit<AccountTypeEntity, 'id'>;
}
