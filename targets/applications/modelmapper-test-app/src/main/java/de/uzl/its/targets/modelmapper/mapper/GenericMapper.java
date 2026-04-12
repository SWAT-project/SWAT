package de.uzl.its.targets.modelmapper.mapper;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import de.uzl.its.targets.modelmapper.dto.BookDto;
import de.uzl.its.targets.modelmapper.model.Book;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

public class GenericMapper<SourceType, DestType> implements Mapper<SourceType, DestType> {

    //@Autowired
    protected ModelMapper modelMapper;
    private Type sourceClass;
    private Type destClass;

    public GenericMapper(Class<SourceType> sourceClass, Class<DestType> destClass) {
        this.sourceClass = sourceClass;
        this.destClass = destClass;
        this.modelMapper = new ModelMapper();
    }

    @Override
    public DestType map2DTO(SourceType fromObj) {
        return modelMapper.map(fromObj, destClass);
    }

    @Override
    public SourceType map2Model(DestType toObj) {
        return modelMapper.map(toObj, sourceClass);
    }

    @Override
    public List<DestType> map2DTOList(List<SourceType> list) {
        return (List<DestType>) list.stream().map(it -> modelMapper.map(it, destClass)).collect(Collectors.toList());
    }

    @Override
    public List<SourceType> map2ModelList(List<DestType> list) {
        return (List<SourceType>) list.stream().map(it -> modelMapper.map(it, sourceClass)).collect(Collectors.toList());
    }

    @Override
    public ModelMapper getMapper() {
        return this.modelMapper;
    }

    public BookDto map2DTOWS(Book fromObj, Boolean status) {
        return modelMapper.map(fromObj, destClass);
    }
}