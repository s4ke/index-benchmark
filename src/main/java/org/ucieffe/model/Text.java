/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.ucieffe.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.genericjpa.annotations.InIndex;

@Entity
@Table(name = "text")
@Indexed(index = "lucene-index")
@InIndex
public class Text {

	private Integer id;
	private String text;

	@Id
	@Column(name = "old_id")
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer oldId) {
		this.id = oldId;
	}

	@Field
	@Column(name = "old_text")
	@Lob
	public String getText() {
		return this.text;
	}

	public void setText(String oldText) {
		this.text = oldText;
	}

	@Override
	public String toString() {
		return "Text [id=" + id + ", text=" + text + "]";
	}

}
