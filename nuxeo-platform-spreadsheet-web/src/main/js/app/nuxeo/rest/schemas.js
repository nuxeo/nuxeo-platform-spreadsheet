/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
import {Request} from './request';

const PATH = '/config/schemas/';

/**
 * REST Schemas
 */
class Schemas extends Request {

  constructor(connection) {
    super(connection, PATH);
    this.data = {};
  }

  fetchFieldsBySchema(schema) {
    return new Promise((resolve, reject) => {
      this.conn.request(`${PATH}/${schema}?fetch.schema=fields`).repositoryName(undefined).get((error, data) => {
        if (error) {
          reject(Error(error));
        }
        let key = data['@prefix'] || data.name;
        this.data[key].fields = data.fields;
        resolve(data);
      });
    });
  }

  fetch(schemas) {
    this.data = [];
    return this.execute().then((entries) => {

      for (let entry of entries) {
        let key = entry['@prefix'] || entry.name;
        if (schemas.indexOf(key) !== -1) {
          this.data[key] = {name: entry.name};
        }
      }

      let promises = [];
      for (let schema of schemas) {
        promises.push(this.fetchFieldsBySchema(this.data[schema].name));
      }

      return Promise.all(promises).then(values => {
        return this.data;
      }, reason => {
        console.log(reason);
      });

    });
  }

}

export {Schemas};
