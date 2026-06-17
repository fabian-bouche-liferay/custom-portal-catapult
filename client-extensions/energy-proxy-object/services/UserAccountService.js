import {logger} from '../util/logger.js';
import APIService from './APIService.js'

class UserAccountService {

    static getUserAccount(bearer, userId) {

        let call = APIService.makeCall(bearer, `/o/headless-admin-user/v1.0/user-accounts//${userId}`, "GET", null);

        return call.then(data => {
            return {
                accountERCs: data.accountBriefs.map(brief => brief.externalReferenceCode)
            };
        }).catch( error => {
            logger.log(error);
            return [];
        });

    }
    
}

export default UserAccountService;