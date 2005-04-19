/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.common;

import java.nio.BufferOverflowException;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.util.Stack;

/**
 * A pooled byte buffer used by MINA applications.
 * <p>
 * This is a replacement for {@link java.nio.ByteBuffer}. Please refer to
 * {@link java.nio.ByteBuffer} and {@link java.nio.Buffer} documentation for
 * usage.  MINA does not use NIO {@link java.nio.ByteBuffer} directly for two
 * reasons:
 * <ul>
 *   <li>It doesn't provide useful getters and putters such as
 *       <code>fill</code>, <code>get/putString</code>, and
 *       <code>get/putAsciiInt()</code> enough.</li>
 *   <li>It is hard to distinguish if the buffer is created from MINA buffer
 *       pool or not.  MINA have to return used buffers back to pool.</li>
 * </ul>
 * <p>
 * You can get a heap buffer from buffer pool:
 * <pre>
 * ByteBuffer buf = ByteBuffer.allocate(1024);
 * </pre>
 * or you can get a direct buffer from buffer pool:
 * <pre>
 * ByteBuffer buf = ByteBuffer.allocate(1024, false);
 * </pre>
 * <p>
 * <b>Please note that you never need to release the allocated buffer because
 * MINA will release it automatically.</b>  But, if you didn't pass it to MINA
 * or called {@link #acquire()} by yourself, you will have to release it manually:
 * <pre>
 * ByteBuffer.release(buf);
 * </pre>
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public abstract class ByteBuffer
{
    private static final int MINIMUM_CAPACITY = 1;

    private static final Stack[] heapBufferStacks = new Stack[] {
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(), };
    
    private static final Stack[] directBufferStacks = new Stack[] {
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(), };
    
    /**
     * Returns the direct or heap buffer which is capable of the specified
     * size.  This method tries to allocate direct buffer first, and then
     * tries heap buffer if direct buffer memory is exhausted.  Please use
     * {@link #allocate(int, boolean)} to allocate buffers of specific type.
     * 
     * @param capacity the capacity of the buffer
     */
    public static ByteBuffer allocate( int capacity )
    {
        try
        {
            // first try to allocate direct buffer
            return allocate( capacity, true );
        }
        catch( OutOfMemoryError e )
        {
            // if failed, try heap
            return allocate( capacity, false );
        }
    }
    
    /**
     * Returns the buffer which is capable of the specified size.
     * 
     * @param capacity the capacity of the buffer
     * @param direct <tt>true</tt> to get a direct buffer,
     *               <tt>false</tt> to get a heap buffer.
     */
    public static ByteBuffer allocate( int capacity, boolean direct )
    {
        Stack[] bufferStacks = direct? directBufferStacks : heapBufferStacks;
        int idx = getBufferStackIndex( bufferStacks, capacity );
        Stack stack = bufferStacks[ idx ];

        DefaultByteBuffer buf;
        synchronized( stack )
        {
            buf = ( DefaultByteBuffer ) stack.pop();
            if( buf == null )
            {
                buf = new DefaultByteBuffer( MINIMUM_CAPACITY << idx, direct );
            }
        }

        buf.clear();
        buf.resetRefCount();

        return buf;
    }
    
    /**
     * Wraps the specified NIO {@link java.nio.ByteBuffer} into MINA buffer.
     */
    public static ByteBuffer wrap( java.nio.ByteBuffer nioBuffer )
    {
        return new DefaultByteBuffer( nioBuffer );
    }
    
    private static int getBufferStackIndex( Stack[] bufferStacks, int size )
    {
        int targetSize = MINIMUM_CAPACITY;
        int stackIdx = 0;
        while( size > targetSize )
        {
            targetSize <<= 1;
            stackIdx ++ ;
            if( stackIdx >= bufferStacks.length )
            {
                throw new IllegalArgumentException(
                        "Buffer size is too big: " + size );
            }
        }

        return stackIdx;
    }

    protected ByteBuffer()
    {
    }

    /**
     * Increases the internal reference count of this buffer to defer
     * automatic release.  You have to invoke {@link #release()} as many
     * as you invoked this method to release this buffer.
     * 
     * @throws IllegalStateException if you attempt to acquire already
     *                               released buffer.
     */
    public abstract void acquire();

    /**
     * Releases the specified buffer to buffer pool.
     * 
     * @throws IllegalStateException if you attempt to release already
     *                               released buffer.
     */
    public abstract void release();

    /**
     * Returns the underlying NIO buffer instance.
     */
    public abstract java.nio.ByteBuffer buf();
    
    public abstract boolean isDirect();
    
    public abstract boolean isReadOnly();
    
    public abstract int capacity();
    
    public abstract int position();

    public abstract ByteBuffer position( int newPosition );

    public abstract int limit();

    public abstract ByteBuffer limit( int newLimit );

    public abstract ByteBuffer mark();

    public abstract ByteBuffer reset();

    public abstract ByteBuffer clear();

    public abstract ByteBuffer flip();

    public abstract ByteBuffer rewind();

    public abstract int remaining();

    public abstract boolean hasRemaining();

    public abstract ByteBuffer slice();

    public abstract ByteBuffer duplicate();

    public abstract ByteBuffer asReadOnlyBuffer();

    public abstract byte get();

    public abstract short getUnsigned();

    public abstract ByteBuffer put( byte b );

    public abstract byte get( int index );

    public abstract short getUnsigned( int index );

    public abstract ByteBuffer put( int index, byte b );

    public abstract ByteBuffer get( byte[] dst, int offset, int length );

    public abstract ByteBuffer get( byte[] dst );

    public abstract ByteBuffer put( java.nio.ByteBuffer src );

    public abstract ByteBuffer put( ByteBuffer src );

    public abstract ByteBuffer put( byte[] src, int offset, int length );

    public abstract ByteBuffer put( byte[] src );

    public abstract ByteBuffer compact();

    public abstract String toString();

    public abstract int hashCode();

    public abstract boolean equals( Object ob );

    public abstract int compareTo( ByteBuffer that );

    public abstract ByteOrder order();

    public abstract ByteBuffer order( ByteOrder bo );

    public abstract char getChar();

    public abstract ByteBuffer putChar( char value );

    public abstract char getChar( int index );

    public abstract ByteBuffer putChar( int index, char value );

    public abstract CharBuffer asCharBuffer();

    public abstract short getShort();

    public abstract int getUnsignedShort();

    public abstract ByteBuffer putShort( short value );

    public abstract short getShort( int index );

    public abstract int getUnsignedShort( int index );

    public abstract ByteBuffer putShort( int index, short value );

    public abstract ShortBuffer asShortBuffer();

    public abstract int getInt();

    public abstract long getUnsignedInt();

    public abstract ByteBuffer putInt( int value );

    public abstract int getInt( int index );

    public abstract long getUnsignedInt( int index );

    public abstract ByteBuffer putInt( int index, int value );

    public abstract IntBuffer asIntBuffer();

    public abstract long getLong();

    public abstract ByteBuffer putLong( long value );

    public abstract long getLong( int index );

    public abstract ByteBuffer putLong( int index, long value );

    public abstract LongBuffer asLongBuffer();

    public abstract float getFloat();

    public abstract ByteBuffer putFloat( float value );

    public abstract float getFloat( int index );

    public abstract ByteBuffer putFloat( int index, float value );

    public abstract FloatBuffer asFloatBuffer();

    public abstract double getDouble();

    public abstract ByteBuffer putDouble( double value );

    public abstract double getDouble( int index );

    public abstract ByteBuffer putDouble( int index, double value );

    public abstract DoubleBuffer asDoubleBuffer();

    /**
     * Returns hexdump of this buffer.
     */
    public abstract String getHexDump();

    ////////////////////////////////
    // String getters and putters //
    ////////////////////////////////

    /**
     * Reads a <code>NUL</code>-terminated string from this buffer and puts it
     * into <code>out</code> using the specified <code>decoder</code>.
     * 
     * @param fieldSize the maximum number of bytes to read
     */
    public abstract ByteBuffer getString( CharBuffer out, int fieldSize,
                                CharsetDecoder decoder );

    /**
     * Reads a <code>NUL</code>-terminated string from this buffer using the
     * specified <code>decoder</code> and returns it.
     * 
     * @param fieldSize the maximum number of bytes to read
     */
    public abstract String getString( int fieldSize, CharsetDecoder decoder );

    /**
     * Writes the content of <code>in</code> into this buffer as a 
     * <code>NUL</code>-terminated string using the specified
     * <code>encoder</code>.
     * <p>
     * If the charset name of the encoder is UTF-16, you cannot specify
     * odd <code>fieldSize</code>, and this method will append two
     * <code>NUL</code>s as a terminator.
     * <p>
     * Please note that this method doesn't terminate with <code>NUL</code>
     * if the input string is too long.
     * 
     * @param fieldSize the maximum number of bytes to write
     */
    public abstract ByteBuffer putString( CharBuffer in, int fieldSize,
                                CharsetEncoder encoder );

    /**
     * Writes the content of <code>in</code> into this buffer as a 
     * <code>NUL</code>-terminated string using the specified
     * <code>encoder</code>.
     * <p>
     * If the charset name of the encoder is UTF-16, you cannot specify
     * odd <code>fieldSize</code>, and this method will append two
     * <code>NUL</code>s as a terminator.
     * <p>
     * Please note that this method doesn't terminate with <code>NUL</code>
     * if the input string is too long.
     * 
     * @param fieldSize the maximum number of bytes to write
     */
    public abstract ByteBuffer putString( CharSequence in, int fieldSize,
                                CharsetEncoder encoder );

    //////////////////////////
    // Skip or fill methods //
    //////////////////////////

    /**
     * Forwards the position of this buffer as the specified <code>size</code>
     * bytes.
     */
    public abstract ByteBuffer skip( int size );

    /**
     * Fills this buffer with the specified value.
     * This method moves buffer position forward.
     */
    public abstract ByteBuffer fill( byte value, int size );

    /**
     * Fills this buffer with the specified value.
     * This method does not change buffer position.
     */
    public abstract ByteBuffer fillAndReset( byte value, int size );

    /**
     * Fills this buffer with <code>NUL (0x00)</code>.
     * This method moves buffer position forward.
     */
    public abstract ByteBuffer fill( int size );

    /**
     * Fills this buffer with <code>NUL (0x00)</code>.
     * This method does not change buffer position.
     */
    public abstract ByteBuffer fillAndReset( int size );
    
    /**
     * Allocates and returns a new {@link ByteBuffer} whose content, position,
     * limit, and capacity is identical.  
     */
    public abstract ByteBuffer fork();
    
    /**
     * Allocates and returns a new {@link ByteBuffer} whose content, position,
     * and limit except capacity is identical.  New capacity can be both greater
     * and less than original capacity.  If limit or position is less than
     * new capacity, they will become same with new capacity.
     */
    public abstract ByteBuffer fork( int newCapacity );
    
    private static abstract class BaseByteBuffer extends ByteBuffer
    {
        private final java.nio.ByteBuffer buf;

        protected BaseByteBuffer( java.nio.ByteBuffer buf )
        {
            if( buf == null )
            {
                throw new NullPointerException( "buf" );
            }
            this.buf = buf;
        }

        public java.nio.ByteBuffer buf()
        {
            return buf;
        }
        
        public boolean isDirect()
        {
            return buf.isDirect();
        }
        
        public boolean isReadOnly()
        {
            return buf.isReadOnly();
        }

        public int capacity()
        {
            return buf.capacity();
        }
        
        public int position()
        {
            return buf.position();
        }

        public ByteBuffer position( int newPosition )
        {
            buf.position( newPosition );
            return this;
        }

        public int limit()
        {
            return buf.limit();
        }

        public ByteBuffer limit( int newLimit )
        {
            buf.limit( newLimit );
            return this;
        }

        public ByteBuffer mark()
        {
            buf.mark();
            return this;
        }

        public ByteBuffer reset()
        {
            buf.reset();
            return this;
        }

        public ByteBuffer clear()
        {
            buf.clear();
            return this;
        }

        public ByteBuffer flip()
        {
            buf.flip();
            return this;
        }

        public ByteBuffer rewind()
        {
            buf.rewind();
            return this;
        }

        public int remaining()
        {
            return buf.remaining();
        }

        public boolean hasRemaining()
        {
            return buf.hasRemaining();
        }

        public ByteBuffer slice()
        {
            return new DuplicateByteBuffer( this, buf.slice());
        }

        public ByteBuffer duplicate()
        {
            return new DuplicateByteBuffer( this, buf.duplicate() );
        }

        public ByteBuffer asReadOnlyBuffer()
        {
            return new DuplicateByteBuffer( this, buf.asReadOnlyBuffer() );
        }

        public byte get()
        {
            return buf.get();
        }

        public short getUnsigned()
        {
            return ( short ) ( get() & 0xff );
        }

        public ByteBuffer put( byte b )
        {
            buf.put( b );
            return this;
        }

        public byte get( int index )
        {
            return buf.get( index );
        }

        public short getUnsigned( int index )
        {
            return ( short ) ( get( index ) & 0xff );
        }

        public ByteBuffer put( int index, byte b )
        {
            buf.put( index, b );
            return this;
        }

        public ByteBuffer get( byte[] dst, int offset, int length )
        {
            buf.get( dst, offset, length );
            return this;
        }

        public ByteBuffer get( byte[] dst )
        {
            buf.get( dst );
            return this;
        }

        public ByteBuffer put( java.nio.ByteBuffer src )
        {
            buf.put( src );
            return this;
        }

        public ByteBuffer put( ByteBuffer src )
        {
            buf.put( src.buf() );
            return this;
        }

        public ByteBuffer put( byte[] src, int offset, int length )
        {
            buf.put( src, offset, length );
            return this;
        }

        public ByteBuffer put( byte[] src )
        {
            buf.put( src );
            return this;
        }

        public ByteBuffer compact()
        {
            buf.compact();
            return this;
        }

        public String toString()
        {
            return buf.toString();
        }

        public int hashCode()
        {
            return buf.hashCode();
        }

        public boolean equals( Object ob )
        {
            if( !( ob instanceof ByteBuffer ) )
                return false;

            ByteBuffer that = ( ByteBuffer ) ob;
            return this.buf.equals( that.buf() );
        }

        public int compareTo( ByteBuffer that )
        {
            return this.buf.compareTo( that.buf() );
        }

        public ByteOrder order()
        {
            return buf.order();
        }

        public ByteBuffer order( ByteOrder bo )
        {
            buf.order( bo );
            return this;
        }

        public char getChar()
        {
            return buf.getChar();
        }

        public ByteBuffer putChar( char value )
        {
            buf.putChar( value );
            return this;
        }

        public char getChar( int index )
        {
            return buf.getChar( index );
        }

        public ByteBuffer putChar( int index, char value )
        {
            buf.putChar( index, value );
            return this;
        }

        public CharBuffer asCharBuffer()
        {
            return buf.asCharBuffer();
        }

        public short getShort()
        {
            return buf.getShort();
        }

        public int getUnsignedShort()
        {
            return getShort() & 0xffff;
        }

        public ByteBuffer putShort( short value )
        {
            buf.putShort( value );
            return this;
        }

        public short getShort( int index )
        {
            return buf.getShort( index );
        }

        public int getUnsignedShort( int index )
        {
            return getShort( index ) & 0xffff;
        }

        public ByteBuffer putShort( int index, short value )
        {
            buf.putShort( index, value );
            return this;
        }

        public ShortBuffer asShortBuffer()
        {
            return buf.asShortBuffer();
        }

        public int getInt()
        {
            return buf.getInt();
        }

        public long getUnsignedInt()
        {
            return getInt() & 0xffffffffL;
        }

        public ByteBuffer putInt( int value )
        {
            buf.putInt( value );
            return this;
        }

        public int getInt( int index )
        {
            return buf.getInt( index );
        }

        public long getUnsignedInt( int index )
        {
            return getInt( index ) & 0xffffffffL;
        }

        public ByteBuffer putInt( int index, int value )
        {
            buf.putInt( index, value );
            return this;
        }

        public IntBuffer asIntBuffer()
        {
            return buf.asIntBuffer();
        }

        public long getLong()
        {
            return buf.getLong();
        }

        public ByteBuffer putLong( long value )
        {
            buf.putLong( value );
            return this;
        }

        public long getLong( int index )
        {
            return buf.getLong( index );
        }

        public ByteBuffer putLong( int index, long value )
        {
            buf.putLong( index, value );
            return this;
        }

        public LongBuffer asLongBuffer()
        {
            return buf.asLongBuffer();
        }

        public float getFloat()
        {
            return buf.getFloat();
        }

        public ByteBuffer putFloat( float value )
        {
            buf.putFloat( value );
            return this;
        }

        public float getFloat( int index )
        {
            return buf.getFloat( index );
        }

        public ByteBuffer putFloat( int index, float value )
        {
            buf.putFloat( index, value );
            return this;
        }

        public FloatBuffer asFloatBuffer()
        {
            return buf.asFloatBuffer();
        }

        public double getDouble()
        {
            return buf.getDouble();
        }

        public ByteBuffer putDouble( double value )
        {
            buf.putDouble( value );
            return this;
        }

        public double getDouble( int index )
        {
            return buf.getDouble( index );
        }

        public ByteBuffer putDouble( int index, double value )
        {
            buf.putDouble( index, value );
            return this;
        }

        public DoubleBuffer asDoubleBuffer()
        {
            return buf.asDoubleBuffer();
        }

        public String getHexDump()
        {
            return ByteBufferHexDumper.getHexdump( this );
        }

        public ByteBuffer getString( CharBuffer out, int fieldSize,
                                    CharsetDecoder decoder )
        {
            checkFieldSize( fieldSize );

            if( fieldSize == 0 )
                return this;

            boolean utf16 = decoder.charset().name().startsWith( "UTF-16" );

            if( utf16 && ( ( fieldSize & 1 ) != 0 ) )
            {
                throw new IllegalArgumentException( "fieldSize is not even." );
            }

            int i;
            int oldLimit = buf.limit();
            int limit = buf.position() + fieldSize;

            if( oldLimit < limit )
            {
                throw new BufferOverflowException();
            }

            buf.mark();

            if( !utf16 )
            {
                for( i = 0; i < fieldSize; i ++ )
                {
                    if( buf.get() == 0 )
                    {
                        break;
                    }
                }

                if( i == fieldSize )
                {
                    buf.limit( limit );
                }
                else
                {
                    buf.limit( buf.position() - 1 );
                }
            }
            else
            {
                for( i = 0; i < fieldSize; i += 2 )
                {
                    if( ( buf.get() == 0 ) && ( buf.get() == 0 ) )
                    {
                        break;
                    }
                }

                if( i == fieldSize )
                {
                    buf.limit( limit );
                }
                else
                {
                    buf.limit( buf.position() - 2 );
                }
            }

            buf.reset();
            decoder.decode( buf, out, true );
            buf.limit( oldLimit );
            buf.position( limit );
            return this;
        }

        public String getString( int fieldSize, CharsetDecoder decoder )
        {
            CharBuffer out = CharBuffer.allocate( ( int ) ( decoder
                    .maxCharsPerByte() * fieldSize ) + 1 );
            getString( out, fieldSize, decoder );
            return out.flip().toString();
        }

        public ByteBuffer putString( CharBuffer in, int fieldSize,
                                    CharsetEncoder encoder )
        {
            checkFieldSize( fieldSize );

            if( fieldSize == 0 )
                return this;

            boolean utf16 = encoder.charset().name().startsWith( "UTF-16" );

            if( utf16 && ( ( fieldSize & 1 ) != 0 ) )
            {
                throw new IllegalArgumentException( "fieldSize is not even." );
            }

            int oldLimit = buf.limit();
            int limit = buf.position() + fieldSize;

            if( oldLimit < limit )
            {
                throw new BufferOverflowException();
            }

            buf.limit( limit );
            encoder.encode( in, buf, true );
            buf.limit( oldLimit );

            if( limit > buf.position() )
            {
                if( !utf16 )
                {
                    buf.put( ( byte ) 0x00 );
                }
                else
                {
                    buf.put( ( byte ) 0x00 );
                    buf.put( ( byte ) 0x00 );
                }
            }

            buf.position( limit );
            return this;
        }

        public ByteBuffer putString( CharSequence in, int fieldSize,
                                    CharsetEncoder encoder )
        {
            return putString( CharBuffer.wrap( in ), fieldSize, encoder );
        }

        public ByteBuffer skip( int size )
        {
            return position( position() + size );
        }

        public ByteBuffer fill( byte value, int size )
        {
            int q = size >>> 3;
            int r = size & 7;

            if( q > 0 )
            {
                int intValue = value | ( value << 8 ) | ( value << 16 )
                               | ( value << 24 );
                long longValue = intValue;
                longValue <<= 32;
                longValue |= intValue;

                for( int i = q; i > 0; i -- )
                {
                    buf.putLong( longValue );
                }
            }

            q = r >>> 2;
            r = r & 3;

            if( q > 0 )
            {
                int intValue = value | ( value << 8 ) | ( value << 16 )
                               | ( value << 24 );
                buf.putInt( intValue );
            }

            q = r >> 1;
            r = r & 1;

            if( q > 0 )
            {
                short shortValue = ( short ) ( value | ( value << 8 ) );
                buf.putShort( shortValue );
            }

            if( r > 0 )
            {
                buf.put( value );
            }

            return this;
        }

        public ByteBuffer fillAndReset( byte value, int size )
        {
            int pos = buf.position();
            try
            {
                fill( value, size );
            }
            finally
            {
                buf.position( pos );
            }
            return this;
        }

        public ByteBuffer fill( int size )
        {
            int q = size >>> 3;
            int r = size & 7;

            for( int i = q; i > 0; i -- )
            {
                buf.putLong( 0L );
            }

            q = r >>> 2;
            r = r & 3;

            if( q > 0 )
            {
                buf.putInt( 0 );
            }

            q = r >> 1;
            r = r & 1;

            if( q > 0 )
            {
                buf.putShort( ( short ) 0 );
            }

            if( r > 0 )
            {
                buf.put( ( byte ) 0 );
            }

            return this;
        }

        public ByteBuffer fillAndReset( int size )
        {
            int pos = buf.position();
            try
            {
                fill( size );
            }
            finally
            {
                buf.position( pos );
            }

            return this;
        }
        
        public ByteBuffer fork()
        {
            return fork( this.capacity() );
        }
        
        public ByteBuffer fork( int newCapacity )
        {
            ByteBuffer buf = allocate( newCapacity );
            int pos = this.position();
            int limit = this.limit();
            this.position( 0 );
            this.limit( newCapacity < this.capacity()? newCapacity : this.capacity() );
            buf.put( this );
            buf.position( pos < newCapacity? pos : newCapacity );
            buf.limit( limit < newCapacity? limit : newCapacity );
            this.limit( this.capacity() );
            this.position( pos );
            this.limit( limit );
            
            return buf;
        }

        private static void checkFieldSize( int fieldSize )
        {
            if( fieldSize < 0 )
            {
                throw new IllegalArgumentException(
                        "fieldSize cannot be negative: " + fieldSize );
            }
        }
    }

    private static class DefaultByteBuffer extends BaseByteBuffer
    {
        private int refCount = 1;
        
        protected DefaultByteBuffer( java.nio.ByteBuffer buf )
        {
            super( buf );
        }

        protected DefaultByteBuffer( int capacity, boolean direct )
        {
            super( direct? java.nio.ByteBuffer.allocateDirect( capacity ) :
                           java.nio.ByteBuffer.allocate( capacity ) );
        }
        
        private synchronized void resetRefCount()
        {
            refCount = 1;
        }
        
        public synchronized void acquire()
        {
            if( refCount <= 0 )
            {
                throw new IllegalStateException( "Already released buffer." );
            }

            refCount ++;
        }

        public synchronized void release()
        {
            if( refCount <= 0 )
            {
                refCount = 0;
                throw new IllegalStateException(
                        "Already released buffer.  You released the buffer too many times." );
            }

            refCount --;
            if( refCount > 0)
            {
                return;
            }

            Stack[] bufferStacks = isDirect()? directBufferStacks : heapBufferStacks;
            Stack stack = bufferStacks[ getBufferStackIndex( bufferStacks, capacity() ) ];
            synchronized( stack )
            {
                // push back
                stack.push( this );
            }
        }
    }
    
    private static class DuplicateByteBuffer extends BaseByteBuffer
    {
        private ByteBuffer buf;

        private DuplicateByteBuffer( ByteBuffer buf, java.nio.ByteBuffer duplicateBuf )
        {
            super( duplicateBuf );
            this.buf = buf;
        }

        public void acquire() {
            buf.acquire();
        }

        public void release() {
            buf.release();
        }
    }
}
