/*
 * Copyright (C) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.reflections.serializers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jboss.errai.reflections.Reflections;
import org.jboss.errai.reflections.ReflectionsException;
import org.jboss.errai.reflections.util.ConfigurationBuilder;
import org.jboss.errai.reflections.util.Utils;

import java.io.*;
import java.util.Map;

/**
 * serialization of Reflections to xml
 * <p/>
 * <p>an example of produced xml:
 * <pre>
 * &#60?xml version="1.0" encoding="UTF-8"?>
 *
 * &#60Reflections>
 *  &#60org.reflections.scanners.MethodAnnotationsScanner>
 *      &#60entry>
 *          &#60key>org.reflections.TestModel$AM1&#60/key>
 *          &#60values>
 *              &#60value>org.reflections.TestModel$C4.m3()&#60/value>
 *              &#60value>org.reflections.TestModel$C4.m1(int[][], java.lang.String[][])&#60/value>
 * ...
 * </pre>
 */
public class XmlSerializer implements Serializer {

  public Reflections read(InputStream inputStream) {
    Reflections reflections = new Reflections(new ConfigurationBuilder()) {
    };

    Document document;
    try {
      document = new SAXReader().read(inputStream);
    }
    catch (DocumentException e) {
      throw new RuntimeException(e);
    }
    for (Object e1 : document.getRootElement().elements()) {
      Element index = (Element) e1;
      for (Object e2 : index.elements()) {
        Element entry = (Element) e2;
        Element key = entry.element("key");
        Element values = entry.element("values");
        for (Object o3 : values.elements()) {
          Element value = (Element) o3;
          Multimap<String, String> stringStringMultimap = reflections.getStore().getStoreMap().get(index.getName());
          if (stringStringMultimap == null) {
            reflections.getStore().getStoreMap().put(index.getName(),
                stringStringMultimap = HashMultimap.<String, String>create());
          }

          stringStringMultimap.put(key.getText(), value.getText());
        }
      }
    }

    return reflections;
  }

  public File save(final Reflections reflections, final String filename) {
    File file = Utils.prepareFile(filename);

    Document document = createDocument(reflections);

    try {
      XMLWriter xmlWriter = new XMLWriter(new FileOutputStream(file), OutputFormat.createPrettyPrint());
      xmlWriter.write(document);
      xmlWriter.close();
    }
    catch (IOException e) {
      throw new ReflectionsException("could not save to file " + filename, e);
    }

    return file;
  }

  public String toString(final Reflections reflections) {
    Document document = createDocument(reflections);

    try {
      StringWriter writer = new StringWriter();
      XMLWriter xmlWriter = new XMLWriter(writer, OutputFormat.createPrettyPrint());
      xmlWriter.write(document);
      xmlWriter.close();
      return writer.toString();
    }
    catch (IOException e) {
      throw new RuntimeException();
    }
  }

  private Document createDocument(final Reflections reflections) {
    final Map<String, Multimap<String, String>> map = reflections.getStore().getStoreMap();

    Document document = DocumentFactory.getInstance().createDocument();
    Element root = document.addElement("Reflections");
    for (String indexName : map.keySet()) {
      Element indexElement = root.addElement(indexName);
      for (String key : map.get(indexName).keySet()) {
        Element entryElement = indexElement.addElement("entry");
        entryElement.addElement("key").setText(key);
        Element valuesElement = entryElement.addElement("values");
        for (String value : map.get(indexName).get(key)) {
          valuesElement.addElement("value").setText(value);
        }
      }
    }
    return document;
  }
}
