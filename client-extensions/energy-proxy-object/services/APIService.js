import config from '../util/configTreePath.js';
import {logger} from '../util/logger.js';
import fetch, { Headers } from 'node-fetch';

class APIService {

    static makeCall(bearer, url, method, body) {

        const lxcDXPMainDomain = config['com.liferay.lxc.dxp.mainDomain'];
        const lxcDXPServerProtocol = config['com.liferay.lxc.dxp.server.protocol'];
        const baseURL = `${lxcDXPServerProtocol}://${lxcDXPMainDomain}`;

        let headers = new Headers();
        headers.set('Authorization', 'Bearer ' + bearer);
        if(body) {
            headers.set('Content-Type', 'application/json');
        }

        logger.log(`${method} ${url}`);

        let call = fetch(baseURL + url, {
            method: method,
            headers: headers,
            ...(body ? { body: JSON.stringify(body) } : {})
        }).then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok ' + response.status + ' ' + response.statusText + ' ' + bearer);
            }

            if (response.status === 204) {
                return null;
            }

            return response.json();
        });

        return call;

    }
    
}

export default APIService;