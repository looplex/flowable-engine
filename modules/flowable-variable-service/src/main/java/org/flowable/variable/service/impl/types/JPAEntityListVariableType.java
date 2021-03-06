/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.variable.service.impl.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;

/**
 * Variable type capable of storing a list of reference to JPA-entities. Only JPA-Entities which are configured by annotations are supported. Use of compound primary keys is not supported. <br>
 * The variable value should be of type {@link List} and can only contain objects of the same type.
 * 
 * @author Frederik Heremans
 */
public class JPAEntityListVariableType implements VariableType, CacheableVariable {

    public static final String TYPE_NAME = "jpa-entity-list";

    protected JPAEntityMappings mappings;

    protected boolean forceCachedValue;

    public JPAEntityListVariableType() {
        mappings = new JPAEntityMappings();
    }

    @Override
    public void setForceCacheable(boolean forceCachedValue) {
        this.forceCachedValue = forceCachedValue;
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public boolean isCachable() {
        return forceCachedValue;
    }

    @Override
    public boolean isAbleToStore(Object value) {
        boolean canStore = false;

        if (value instanceof List<?>) {
            List<?> list = (List<?>) value;
            if (list.size() > 0) {
                // We can only store the list if we are sure it's actually a
                // list of JPA entities. In case the
                // list is empty, we don't store it.
                canStore = true;
                Class<?> entityClass = mappings.getEntityMetaData(list.get(0).getClass()).getEntityClass();

                for (Object entity : list) {
                    canStore = entity != null && mappings.isJPAEntity(entity) && mappings.getEntityMetaData(entity.getClass()).getEntityClass().equals(entityClass);
                    if (!canStore) {
                        // In case the object is not a JPA entity or the class
                        // doesn't match, we can't store the list
                        break;
                    }
                }
            }
        }
        return canStore;
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        EntityManagerSession entityManagerSession = Context.getCommandContext().getSession(EntityManagerSession.class);
        if (entityManagerSession == null) {
            throw new FlowableException("Cannot set JPA variable: " + EntityManagerSession.class + " not configured");
        } else {
            // Before we set the value we must flush all pending changes from
            // the entitymanager
            // If we don't do this, in some cases the primary key will not yet
            // be set in the object
            // which will cause exceptions down the road.
            entityManagerSession.flush();
        }

        if (value instanceof List<?> && ((List<?>) value).size() > 0) {
            List<?> list = (List<?>) value;
            List<String> ids = new ArrayList<>();

            String type = mappings.getJPAClassString(list.get(0));
            for (Object entry : list) {
                ids.add(mappings.getJPAIdString(entry));
            }

            // Store type in text field and the ID's as a serialized array
            valueFields.setBytes(serializeIds(ids));
            valueFields.setTextValue(type);

        } else if (value == null) {
            valueFields.setBytes(null);
            valueFields.setTextValue(null);
        } else {
            throw new FlowableIllegalArgumentException("Value is not a list of JPA entities: " + value);
        }

    }

    @Override
    public Object getValue(ValueFields valueFields) {
        byte[] bytes = valueFields.getBytes();
        if (valueFields.getTextValue() != null && bytes != null) {
            String entityClass = valueFields.getTextValue();

            List<Object> result = new ArrayList<>();
            String[] ids = deserializeIds(bytes);

            for (String id : ids) {
                result.add(mappings.getJPAEntity(entityClass, id));
            }

            return result;
        }
        return null;
    }

    /**
     * @return a bytearray containing all ID's in the given string serialized as an array.
     */
    protected byte[] serializeIds(List<String> ids) {
        try {
            String[] toStore = ids.toArray(new String[] {});
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);

            out.writeObject(toStore);
            return baos.toByteArray();
        } catch (IOException ioe) {
            throw new FlowableException("Unexpected exception when serializing JPA id's", ioe);
        }
    }

    protected String[] deserializeIds(byte[] bytes) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bais);

            Object read = in.readObject();
            if (!(read instanceof String[])) {
                throw new FlowableIllegalArgumentException("Deserialized value is not an array of ID's: " + read);
            }

            return (String[]) read;
        } catch (IOException | ClassNotFoundException ioe) {
            throw new FlowableException("Unexpected exception when deserializing JPA id's", ioe);
        }
    }

}
