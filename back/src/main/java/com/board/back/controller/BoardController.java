package com.board.back.controller;

import com.board.back.form.BoardFileForm;
import com.board.back.form.condition.BoardSearchCondition;
import com.board.back.form.validation.BoardDeleteForm;
import com.board.back.form.validation.BoardFileDeleteForm;
import com.board.back.form.validation.BoardSaveForm;
import com.board.back.form.validation.BoardUpdateForm;
import com.board.back.repository.BoardMainRepository;
import com.board.back.service.BoardService;
import com.board.back.util.FileUtil;
import com.board.back.util.JwtUtil;
import com.board.back.util.TokenRequestFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@CrossOrigin
@RestController
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;

    private final BoardMainRepository boardMainRepository;

    private final JwtUtil jwtUtil;

    private final TokenRequestFilter tokenRequestFilter;

    private final FileUtil fileUtil;

    /**
     * 목록 조회
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> boardList(Pageable pageable,
                                    BoardSearchCondition searchCondition,
                                    @RequestParam(value = "boardMainIdx", required = true) Long boardMainIdx) {
        Map<String, Object> result = new HashMap<>();
        result.put("boardList", boardService.getBoardList(pageable, searchCondition, boardMainIdx));
        return ResponseEntity.ok(result);
    }

    /**
     * 등록 폼 조회(빈 화면, 게시판 카테고리 가져오기)
     */
    @GetMapping("/add")
    public ResponseEntity<Map<String, Object>> boardAddForm() {
        Map<String, Object> result = new HashMap<>();
        result.put("boardMainList", boardMainRepository.findAll()); //게시판 카테고리 조회
        return ResponseEntity.ok(result);
    }

    /**
     * 등록
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> boardAdd(@RequestPart("body") @Valid BoardSaveForm saveForm,
                                                        @RequestPart(value = "file", required = false) List<MultipartFile> file,
                                                        BindingResult bindingResult) throws Exception {
        Map<String, Object> result = new HashMap<>();

        if (bindingResult.hasErrors()) {
            FieldError error = bindingResult.getFieldErrors().get(0);
            throw new Exception(error.getDefaultMessage());
        } else {
            List<BoardFileForm> fileForm = new ArrayList<>();
            if (file != null && !file.isEmpty()) {
                fileForm = fileUtil.saveFiles(file, String.valueOf(saveForm.getBoardMainIdx()));
            }
            boardService.regBoardInfo(saveForm, fileForm);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 수정 폼 조회
     */
    @GetMapping("/edit/{boardIdx}")
    public ResponseEntity<Map<String, Object>> boardEditForm(@PathVariable Long boardIdx) {
        Map<String, Object> result = new HashMap<>();
        result.put("boardMainList", boardMainRepository.findAll());
        result.put("boardInfo", boardService.getBoardInfo(boardIdx));
        return ResponseEntity.ok(result);
    }

    /**
     * 수정
     */
    @PostMapping("/edit")
    public ResponseEntity<Map<String, Object>> boardEdit(@RequestPart(value = "body") @Valid BoardUpdateForm updateForm,
                                                         @RequestPart(value = "file", required = false) List<MultipartFile> file,
                                                         HttpServletRequest request,
                                                         BindingResult bindingResult) throws Exception {
        Map<String, Object> result = new HashMap<>();

        //Spring security 에 세팅된 회원명으로 비교 시 사용
        //String securityUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        //token 작성자 확인(작성자만 수정 가능하도록)
        String tokenUserId = decodeToken(request);
        if (!updateForm.getRegUserId().equals(tokenUserId)) {
            throw new Exception("글 수정 권한이 없습니다.");
        }

        if (bindingResult.hasErrors()) {
            FieldError error = bindingResult.getFieldErrors().get(0);
            throw new Exception(error.getDefaultMessage());
        } else {
            List<BoardFileForm> fileForm = new ArrayList<>();
            if (file != null && !file.isEmpty()) {
                fileForm = fileUtil.saveFiles(file, String.valueOf(updateForm.getBoardMainIdx()));
            }
            boardService.modBoardInfo(updateForm, fileForm);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 삭제
     */
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> boardDelete(@RequestBody @Valid BoardDeleteForm deleteForm,
                                                           HttpServletRequest request,
                                                           BindingResult bindingResult) throws Exception {
        Map<String, Object> result = new HashMap<>();

        //token 작성자 확인(작성자만 삭제 가능하도록)
        String tokenUserId = decodeToken(request);
        if (!deleteForm.getRegUserId().equals(tokenUserId)) {
            throw new Exception("글 수정 권한이 없습니다.");
        }
        if (bindingResult.hasErrors()) {
            FieldError error = bindingResult.getFieldErrors().get(0);
            throw new Exception(error.getDefaultMessage());
        } else {
            boardService.delBoardInfo(deleteForm);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 게시판 업로드 파일 개별 삭제
     */
    @PostMapping("/file/delete")
    public ResponseEntity<Map<String, Object>> boardFileDelete(@RequestBody @Valid BoardFileDeleteForm boardFileDeleteForm,
                                                               HttpServletRequest request,
                                                               BindingResult bindingResult) throws Exception {
        Map<String, Object> result = new HashMap<>();

        //token 작성자 확인(작성자만 삭제 가능하도록)
        String tokenUserId = decodeToken(request);
        if (!boardFileDeleteForm.getRegUserId().equals(tokenUserId)) {
            throw new Exception("글 수정 권한이 없습니다.");
        }
        if (bindingResult.hasErrors()) {
            FieldError error = bindingResult.getFieldErrors().get(0);
            throw new Exception(error.getDefaultMessage());
        } else {
            boardService.delBoardFileInfo(boardFileDeleteForm.getBoardFileIdx());
        }
        return ResponseEntity.ok(result);
    }

    private String decodeToken(HttpServletRequest request) {
        return jwtUtil.getUserFromToken(tokenRequestFilter.parseJwt(request));
    }

}
