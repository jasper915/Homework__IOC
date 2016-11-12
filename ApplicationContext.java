import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class ApplicationContext {
	// ���dom
	private Document dom;
	private Element root;
	// �����������ɵ�bean����map��
	private HashMap<String, Object> beans = new HashMap<String, Object>();

	public void ClassPathXmlApplicationContext(String xmlUrl){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder db;
		try {
			db = factory.newDocumentBuilder();
			dom = db.parse(xmlUrl);
			dom.normalize();
			root = dom.getDocumentElement();
			System.out.println("��ʼ����ʼ...");
			initBeans();
			System.out.println("��ʼ������...");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	// ��ʼ�������ļ������з������ص�bean
	private void initBeans() {
		for (Node node = root.getFirstChild(); node != null; node = node
				.getNextSibling()) {
			// �ӳټ���Bean
			if (node.getNodeName().equals("bean")
					&& ((Element) node).hasAttribute("lazy-init")
					&& ((Element) node).getAttribute("lazy-init")
							.equals("true"))
				continue;
			if (node.getNodeType() == Node.ELEMENT_NODE)
				// �����󣬴���map��
				beans.put(((Element) node).getAttribute("id"), initBean(node));
		}
	}

	private Object initBean(Node node) {
		// ��ʼ��������
		Class c = null;
		// ��Ҫ��ʼ����bean
		Object object = null;
		// ���ɶ���
		try {
			c = Class.forName(((Element) node).getAttribute("class"));
			object = c.newInstance();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		// �����beanΪ��������bean
		if (isPrimitive(((Element) node).getAttribute("class"))
				|| ((Element) node).hasAttribute("value")) {
			object = getInstanceForName(((Element) node).getAttribute("class"),
					((Element) node).getAttribute("value"));
			return object;
		}


		//boolean constructorInit = false;//�Ƿ�Ϊ���캯��ע��
		ArrayList<Class> parameterTypes = new ArrayList<Class>();
		ArrayList<Object> parameters = new ArrayList<Object>();
		// Ϊbean��������
		for (Node property = node.getFirstChild(); property != null; property = property
				.getNextSibling()) {
			if (property.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			if (property.getNodeName().equals("constructor-arg")) {
				// beanΪ���캯��ע��
				//constructorInit = true;
				if (((Element)property).hasAttribute("value")) {
					//�ò���Ϊ�������Ͳ���
					getInstanceForName(((Element)property).getAttribute("type"), ((Element)property).getAttribute("value"));
					Class temp = nameToPrimitiveClass(((Element)property).getAttribute("type"));
					parameterTypes.add(temp);
					parameters.add(getInstanceForName(((Element)property).getAttribute("type"), ((Element)property).getAttribute("value")));
				} else if (((Element)property).hasAttribute("ref")){
					String refId = ((Element) property).getAttribute("ref");
					// �ò���Ϊ�ⲿbean��������
					if (beans.containsKey(refId)) {
						// ��beans��map�з����ѳ�ʼ������bean
						parameterTypes.add(beans.get(refId).getClass());
						parameters.add(beans.get(refId));
					} else {
						// δ��ʼ������bean
						Node e = dom.getElementById(refId);
						if (e.getNodeName().equals("bean")) {
							// �ݹ���ã���ʼ�������õ�bean
							Class temp = null;
							try {
								temp = Class.forName(((Element)e).getAttribute("class"));
							} catch (ClassNotFoundException e1) {
								e1.printStackTrace();
							}
							parameterTypes.add(temp);
							parameters.add(initBean(e));
						}
					}
				}
			} 

				
			}
		

	
		
			Class[] aClasses = {};
			Class[] classes = (Class[]) parameterTypes.toArray(aClasses);
			Object[] prams = (Object[])parameters.toArray();
			try {
				object = c.getConstructor(classes).newInstance(prams);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		

		return object;
	}

	// ��ȡhashMap����Ӧ��bean
	public Object getBean(String id) {
		Object bean = beans.get(id);
		// δ��ʼ����bean���п���Ϊ�����أ�Ҳ�п���û�д�bean
		if (bean == null) {
			Element element = dom.getElementById(id);
			if (element.getTagName().equals("bean")
					&& element.hasAttribute("lazy-init")
					&& element.getAttribute("lazy-init").equals("true")) {
				// ȷʵΪ������
				// �����󣬴���map��
				System.out.println("�����أ�" + id);
				bean = initBean(element);
				beans.put(element.getAttribute("id"), bean);
			} else {
				// �޴�Bean������������
				System.out.println("�Ҳ�����Bean");
			}
		}
		return bean;
	}

	// �����Ƿ�Ϊ����ֵ���ͻ��������
	@SuppressWarnings("rawtypes")
	private boolean isPrimitive(String className) {
		Class clazz = null;
		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		String name = clazz.getName();
		if (clazz.isPrimitive() || name.equals("java.lang.String")
				|| name.equals("java.lang.Integer")
				|| name.equals("java.lang.Float")
				|| name.equals("java.lang.Double")
				|| name.equals("java.lang.Character")
				|| name.equals("java.lang.Integer")
				|| name.equals("java.lang.Boolean")
				|| name.equals("java.lang.Short")) {
			return true;
		} else {
			return false;
		}
	}

	// ͨ���ַ����������ͣ������˶Ի������͵İ�װ
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object getInstanceForName(String name, String value) {
		Class clazz = nameToClass(name);

		Object object = null;
		try {
			object = clazz.getConstructor(String.class).newInstance(value);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return object;
	}

	@SuppressWarnings("rawtypes")
	private Class nameToPrimitiveClass(String name){
		Class clazz = null;
		if (name.equals("int")) {
			clazz = int.class;
		} else if (name.equals("char")) {
			clazz = char.class;
		} else if (name.equals("boolean")) {
			clazz = boolean.class;
		} else if (name.equals("short")) {
			clazz = short.class;
		} else if (name.equals("long")) {
			clazz = long.class;
		} else if (name.equals("float")) {
			clazz = float.class;
		} else if (name.equals("double")) {
			clazz = double.class;
		} else if (name.equals("byte")) {
			clazz = byte.class;
		} else {
			try {
				clazz = Class.forName(name);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		return clazz;
	}

	@SuppressWarnings("rawtypes")
	private Class nameToClass(String name){
		Class clazz = null;
		if (name.equals("int")) {
			clazz = Integer.class;
		} else if (name.equals("char")) {
			clazz = Character.class;
		} else if (name.equals("boolean")) {
			clazz = Boolean.class;
		} else if (name.equals("short")) {
			clazz = Short.class;
		} else if (name.equals("long")) {
			clazz = Long.class;
		} else if (name.equals("float")) {
			clazz = Float.class;
		} else if (name.equals("double")) {
			clazz = Double.class;
		} else if (name.equals("byte")) {
			clazz = Byte.class;
		} else {
			try {
				clazz = Class.forName(name);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		return clazz;
	}

}
