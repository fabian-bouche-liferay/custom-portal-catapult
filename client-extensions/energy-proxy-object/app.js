/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import express from 'express';

import config from './util/configTreePath.js';

import {
	corsWithReady,
	liferayJWT,
} from './util/liferay-oauth2-resource-server.js';

import {logger} from './util/logger.js';

import EnergyDataMock from './services/EnergyDataMock.js'

const serverPort = config['server.port'];
const app = express();

logger.log(`config: ${JSON.stringify(config, null, '\t')}`);

app.use(express.json());
app.use(corsWithReady);
app.use(liferayJWT);

app.get(config.readyPath, (req, res) => {
	res.send('READY');
});

app.get('/energy/objectEntryManager/:objectDefinitionExternalReferenceCode', async (req, res) => {

    const idToken = req.headers['x-upstream-id-token'];

    console.log('Upstream ID Token:', idToken);

    console.log("Object definition ERC: " + req.params.objectDefinitionExternalReferenceCode);

    const { companyId, languageId, scopeKey, userId, page, pageSize } = req.query;
    let result = await EnergyDataMock.getEnergyConsumptionAtPageIndex(page - 1, pageSize);
    let resultPage = {
        items:		result,
        page:		page,
        totalCount:	result.length,
        pageSize:	pageSize
    }
    console.log(resultPage);
	res.status(200).send(resultPage);

});

app.get('/energy/objectEntryManager/:objectDefinitionExternalReferenceCode/:objectEntryExternalReferenceCode', async (req, res) => {

    const idToken = req.headers['x-upstream-id-token'];

    console.log('Upstream ID Token:', idToken);

    console.log("Object definition ERC: " + req.params.objectDefinitionExternalReferenceCode);
    console.log("Object entry ERC: " + req.params.objectEntryExternalReferenceCode);

    const extractedData = extractCustomerRefAndDate(req.params.objectEntryExternalReferenceCode);
    console.log(extractedData);

    const { companyId, languageId, scopeKey, userId, page, pageSize } = req.query;
    let result = await EnergyDataMock.getEnergyConsumptionByCustomerAndDate(extractedData.customerRef, extractedData.date);
    console.log(result);
	res.status(200).send(result);

});


function extractCustomerRefAndDate(externalReferenceCode) {
    const match = externalReferenceCode.match(/^(.+)-(\d{6})$/);
    if (match) {
        const customerRef = match[1];
        const year = match[2].substring(0, 4);
        const month = match[2].substring(4, 6);
        return {
            customerRef,
            date: new Date(`${year}-${month}-01`)
        };
    }
    return null;
}

app.listen(serverPort, () => {
	logger.log(`App listening on ${serverPort}`);
});

export default app;