/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.json;

import java.util.HashMap;
import java.util.Map;

/**
 * SchemaObject
 */
public class SchemaObject implements IState
{
	private Schema _owningSchema;
	private Map<String, SchemaProperty> _properties;
	private boolean _inObject;
	private boolean _inProperty;

	SchemaObject(Schema owningSchema)
	{
		this._owningSchema = owningSchema;
	}

	/**
	 * addProperty
	 * 
	 * @param name
	 * @param typeName
	 */
	public void addProperty(String name, String typeName)
	{
		this.addProperty(this._owningSchema.createProperty(name, typeName));
	}

	/**
	 * addProperty
	 * 
	 * @param property
	 */
	public void addProperty(SchemaProperty property)
	{
		if (this._properties == null)
		{
			this._properties = new HashMap<String, SchemaProperty>();
		}

		this._properties.put(property.getName(), property);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.json.State#enter()
	 */
	public void enter()
	{
		this._inObject = false;
		this._inProperty = false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.json.State#exit()
	 */
	public void exit()
	{
	}

	/**
	 * getOwningSchema
	 * 
	 * @return
	 */
	public Schema getOwningSchema()
	{
		return this._owningSchema;
	}

	/**
	 * getProperty
	 * 
	 * @param name
	 * @return
	 */
	public SchemaProperty getProperty(String name)
	{
		SchemaProperty result = null;

		if (this._properties != null)
		{
			result = this._properties.get(name);
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.json.IState#isValidTransition(com.aptana.json.EventType, java.lang.Object)
	 */
	public boolean isValidTransition(SchemaEventType event, Object value)
	{
		boolean result = false;

		switch (event)
		{
			case START_OBJECT:
				result = (this._inObject == false);
				break;

			case START_OBJECT_ENTRY:
				result = (this._inObject && this._inProperty == false && value != null && value.toString().length() > 0);

				// TODO: check property name?
				break;

			case END_OBJECT:
				result = (this._inProperty == false && this._inObject);
				break;

			case END_OBJECT_ENTRY:
				result = (this._inObject && this._inProperty);
				break;
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.json.State#transition(com.aptana.json.Context, com.aptana.json.EventType, java.lang.Object)
	 */
	public void transition(ISchemaContext context, SchemaEventType event, Object value)
	{
		switch (event)
		{
			case START_OBJECT:
				if (this._inObject)
				{
					throw new IllegalStateException("Attempted to start and object that has already been started");
				}

				this._inObject = true;
				break;

			case START_OBJECT_ENTRY:
				if (this._inObject == false)
				{
					throw new IllegalStateException("Attempted to start an object entry in an object that has not been started");
				}
				if (this._inProperty)
				{
					throw new IllegalStateException("Attempted to start an object entry that has already been started");
				}
				if (value == null || value.toString().length() == 0)
				{
					throw new IllegalStateException("Attempted to start an object entry without providing its name");
				}

				String name = value.toString();
				SchemaProperty property = this.getProperty(name);

				if (property == null)
				{
					throw new IllegalStateException("Attempted to start an object entry that does not exist in this object: " + name);
				}

				this._inProperty = true;
				context.pushType(property.getType());
				break;

			case END_OBJECT:
				if (this._inProperty)
				{
					throw new IllegalStateException("Attempted to end an object that has an open object entry");
				}
				if (this._inObject == false)
				{
					throw new IllegalStateException("Attempted to end an object that is already ended");
				}

				this._inObject = false;
				context.popType();
				break;

			case END_OBJECT_ENTRY:
				if (this._inObject == false)
				{
					throw new IllegalStateException("Attempted to end an object entry in an object that has not been started");
				}
				if (this._inProperty == false)
				{
					throw new IllegalStateException("Attempted to end an object entry that has not been started");
				}

				this._inProperty = false;
				break;

			default:
				throw new IllegalStateException("Unsupported event type: " + event.name());
		}
	}
}
