/*
 * JBossMQ, the OpenSource JMS implementation
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.jboss.jms.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotWriteableException;
import javax.jms.ObjectMessage;

import org.jboss.util.Classes;

/**
 * This class implements javax.jms.ObjectMessage ported from SpyObjectMessage in JBossMQ.
 *
 * @author Norbert Lataille (Norbert.Lataille@m4x.org)
 * @author <a href="mailto:adrian@jboss.org">Adrian Brock</a>
 * @version $Revision$
 */
public class JBossObjectMessage extends JBossMessage implements ObjectMessage
{
   // Constants -----------------------------------------------------

	private static final long serialVersionUID = -1626960567569667875L;
   
   // Attributes ----------------------------------------------------

  
	/** Is it a byte array */
   boolean isByteArray = false;
   /** The bytes */
   byte[] objectBytes = null;
   
   // Static --------------------------------------------------------
   
   // Constructors --------------------------------------------------
   
     
   // Public --------------------------------------------------------
   
   // ObjectMessage implementation ----------------------------------

   public void setObject(Serializable object) throws JMSException
   {
      if (!messageReadWrite)
        {
         throw new MessageNotWriteableException("setObject");
      }
      if (object == null)
      {
         objectBytes = null;
         return;
      }
      try
      {
         if (object instanceof byte[])
           {
            //cheat for byte arrays
            isByteArray = true;
            objectBytes = new byte[((byte[]) object).length];
            System.arraycopy(object, 0, objectBytes, 0, objectBytes.length);
         }
         else
           {
            isByteArray = false;
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            ObjectOutputStream objectOut = new ObjectOutputStream(byteArray);
            objectOut.writeObject(object);
            objectBytes = byteArray.toByteArray();
            objectOut.close();
         }
      }
      catch (IOException e)
      {
         throw new MessageFormatException("Object cannot be serialized");
      }
   }

   public Serializable getObject() throws JMSException
   {

      Serializable retVal = null;
      try
      {
         if (null != objectBytes)
         {
            if (isByteArray)
            {
               retVal = new byte[objectBytes.length];
               System.arraycopy(objectBytes, 0, retVal, 0, objectBytes.length);
            }
            else
            {

               /**
                * Default implementation ObjectInputStream does not work well
                * when running an a micro kernal style app-server like JBoss.
                * We need to look for the Class in the context class loader and
                * not in the System classloader.
                * 
                * Would this be done better by using a MarshaedObject??
                */
               class ObjectInputStreamExt extends ObjectInputStream
               {
                  ObjectInputStreamExt(InputStream is) throws IOException
                  {
                     super(is);
                  }

                  protected Class resolveClass(ObjectStreamClass v) throws IOException, ClassNotFoundException
                  {
                     return Classes.loadClass(v.getName());
                  }
               }
               ObjectInputStream input = new ObjectInputStreamExt(new ByteArrayInputStream(objectBytes));
               retVal = (Serializable) input.readObject();
               input.close();
            }
         }
      }
      catch (ClassNotFoundException e)
      {
         throw new MessageFormatException("ClassNotFoundException: " + e.getMessage());
      }
      catch (IOException e)
      {
         throw new MessageFormatException("IOException: " + e.getMessage());
      }
      return retVal;
   }
   
   // JBossMessage overrides ----------------------------------------

   public void clearBody() throws JMSException
   {
      objectBytes = null;
      super.clearBody();
   }

  
   
   // Externalizable implementation ---------------------------------


   //TODO
   
   
   /*
   public void writeExternal(ObjectOutput out) throws IOException
   {
      super.writeExternal(out);
      out.writeBoolean(isByteArray);
      if (objectBytes == null)
      {
         out.writeInt(-1);
      }
      else
      {
         out.writeInt(objectBytes.length);
         out.write(objectBytes);
      }
   }

   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      super.readExternal(in);
      isByteArray = in.readBoolean();
      int length = in.readInt();
      if (length < 0)
      {
         objectBytes = null;
      }
      else
      {
         objectBytes = new byte[length];
         in.readFully(objectBytes);
      }
   }
   
   */
   
   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------
}
